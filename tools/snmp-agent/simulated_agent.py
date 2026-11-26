#!/usr/bin/env python3
"""
本机模拟 SNMP v2c Agent（仅用于毕业设计测试）。

只实现 SNMPv2c GET，支持三个 OID：
  1.3.6.1.2.1.1.1.0  sysDescr
  1.3.6.1.2.1.1.3.0  sysUpTime
  1.3.6.1.2.1.1.5.0  sysName

不依赖第三方库，使用 Python 标准库解析/构造最基本的 BER 编码。
仅监听回环地址或指定测试地址，不要暴露到公网。

示例：
  python simulated_agent.py --host 127.0.0.1 --port 1161 --community sim-public
"""

import argparse
import logging
import socketserver

LOG = logging.getLogger("simulated-snmp-agent")

OID_SYS_DESCR = "1.3.6.1.2.1.1.1.0"
OID_SYS_UPTIME = "1.3.6.1.2.1.1.3.0"
OID_SYS_NAME = "1.3.6.1.2.1.1.5.0"


def read_tlv(data, offset):
    if offset >= len(data):
        raise ValueError("unexpected end of BER data")
    tag = data[offset]
    offset += 1
    if offset >= len(data):
        raise ValueError("missing BER length")
    first = data[offset]
    offset += 1
    if first & 0x80:
        count = first & 0x7F
        if count == 0 or offset + count > len(data):
            raise ValueError("invalid BER length")
        length = int.from_bytes(data[offset:offset + count], "big")
        offset += count
    else:
        length = first
    if offset + length > len(data):
        raise ValueError("BER value exceeds packet")
    return tag, data[offset:offset + length], offset + length


def encode_length(length):
    if length < 0x80:
        return bytes([length])
    encoded = length.to_bytes((length.bit_length() + 7) // 8, "big")
    return bytes([0x80 | len(encoded)]) + encoded


def tlv(tag, value):
    return bytes([tag]) + encode_length(len(value)) + value


def decode_oid(value):
    if not value:
        raise ValueError("empty OID")
    first = value[0]
    arcs = [first // 40, first % 40]
    number = 0
    for byte in value[1:]:
        number = (number << 7) | (byte & 0x7F)
        if not byte & 0x80:
            arcs.append(number)
            number = 0
    return ".".join(str(arc) for arc in arcs)


def encode_oid(oid):
    arcs = [int(part) for part in oid.split(".")]
    if len(arcs) < 2:
        raise ValueError("OID needs at least two arcs")
    encoded = bytearray([arcs[0] * 40 + arcs[1]])
    for arc in arcs[2:]:
        parts = [arc & 0x7F]
        arc >>= 7
        while arc:
            parts.append((arc & 0x7F) | 0x80)
            arc >>= 7
        encoded.extend(reversed(parts))
    return bytes(encoded)


def encode_int(value):
    if value == 0:
        return b"\x00"
    length = (value.bit_length() + 7) // 8
    return value.to_bytes(length, "big", signed=False)


class SimulatedAgent(socketserver.BaseRequestHandler):
    community = "sim-public"
    sys_name = "simulated-core-switch"
    sys_descr = "Simulated SNMP v2c Agent for thesis testing"
    uptime_ticks = 123456

    def handle(self):
        data, sock = self.request
        try:
            response = self.build_response(data)
        except Exception as exc:
            LOG.warning("无法解析 SNMP 请求：%s", exc)
            return
        if response is not None:
            sock.sendto(response, self.client_address)
            LOG.info("响应 %s:%s，长度=%s", self.client_address[0], self.client_address[1], len(response))

    def build_response(self, packet):
        tag, seq, _ = read_tlv(packet, 0)
        if tag != 0x30:
            return None

        offset = 0
        _, version_raw, offset = read_tlv(seq, offset)
        _, community_raw, offset = read_tlv(seq, offset)
        pdu_tag, pdu_raw, offset = read_tlv(seq, offset)

        if int.from_bytes(version_raw, "big") != 1:
            LOG.warning("只支持 SNMPv2c(version=1)")
            return None
        if community_raw.decode("utf-8", errors="replace") != self.community:
            LOG.warning("社区字符串不匹配，丢弃请求")
            return None
        if pdu_tag not in (0xA0, 0xA1):
            LOG.warning("只支持 GET/GETNEXT，收到 tag=0x%02X", pdu_tag)
            return None

        pdu_offset = 0
        _, request_id_raw, pdu_offset = read_tlv(pdu_raw, pdu_offset)
        _, _, pdu_offset = read_tlv(pdu_raw, pdu_offset)
        _, _, pdu_offset = read_tlv(pdu_raw, pdu_offset)
        _, varbind_list, pdu_offset = read_tlv(pdu_raw, pdu_offset)

        varbinds = []
        vb_offset = 0
        while vb_offset < len(varbind_list):
            _, vb_raw, vb_offset = read_tlv(varbind_list, vb_offset)
            inner_offset = 0
            _, oid_raw, inner_offset = read_tlv(vb_raw, inner_offset)
            oid = decode_oid(oid_raw)
            varbinds.append(oid)

        response_varbinds = b"".join(
            tlv(0x30, tlv(0x06, encode_oid(oid)) + self.value_for(oid))
            for oid in varbinds
        )
        response_pdu = tlv(
            0xA2,
            tlv(0x02, request_id_raw)
            + tlv(0x02, b"\x00")
            + tlv(0x02, b"\x00")
            + tlv(0x30, response_varbinds),
        )
        return tlv(0x30, tlv(0x02, b"\x01") + tlv(0x04, self.community.encode()) + response_pdu)

    def value_for(self, oid):
        if oid == OID_SYS_DESCR:
            return tlv(0x04, self.sys_descr.encode("utf-8"))
        if oid == OID_SYS_NAME:
            return tlv(0x04, self.sys_name.encode("utf-8"))
        if oid == OID_SYS_UPTIME:
            return tlv(0x43, self.uptime_ticks.to_bytes(4, "big"))
        return tlv(0x05, b"")


class ThreadingUdpServer(socketserver.ThreadingUDPServer):
    allow_reuse_address = True
    daemon_threads = True


def main():
    parser = argparse.ArgumentParser(description="本机模拟 SNMP v2c Agent")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=1161)
    parser.add_argument("--community", default="sim-public")
    parser.add_argument("--sys-name", default="simulated-core-switch")
    parser.add_argument("--sys-descr", default="Simulated SNMP v2c Agent for thesis testing")
    parser.add_argument("--uptime-ticks", type=int, default=123456)
    args = parser.parse_args()

    SimulatedAgent.community = args.community
    SimulatedAgent.sys_name = args.sys_name
    SimulatedAgent.sys_descr = args.sys_descr
    SimulatedAgent.uptime_ticks = args.uptime_ticks

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    with ThreadingUdpServer((args.host, args.port), SimulatedAgent) as server:
        LOG.info("模拟 SNMP Agent 监听 %s:%s，community=%s，sysName=%s",
                 args.host, args.port, "***", args.sys_name)
        server.serve_forever()


if __name__ == "__main__":
    main()
