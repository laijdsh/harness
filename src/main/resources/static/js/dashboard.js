(function () {
    'use strict';

    const charts = {};
    const COLOR = {
        online: '#16a34a',
        offline: '#dc2626',
        unknown: '#64748b',
        error: '#d97706',
        open: '#dc2626',
        acknowledged: '#d97706',
        recovered: '#16a34a',
        blue: '#2563eb'
    };

    function chart(id) {
        const element = document.getElementById(id);
        if (!element) {
            return null;
        }
        if (!charts[id]) {
            charts[id] = echarts.init(element);
        }
        return charts[id];
    }

    async function getJson(url) {
        const response = await fetch(url, { headers: { Accept: 'application/json' } });
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        return response.json();
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value == null ? '--' : String(value);
        }
    }

    function formatTime(value) {
        if (!value) {
            return '--';
        }
        return value.replace('T', ' ').slice(0, 19);
    }

    function renderDeviceStatus(summary) {
        const instance = chart('device-status-chart');
        if (!instance) {
            return;
        }

        const data = [
            { name: '在线', value: summary.online, itemStyle: { color: COLOR.online } },
            { name: '离线', value: summary.offline, itemStyle: { color: COLOR.offline } },
            { name: '未知', value: summary.unknown, itemStyle: { color: COLOR.unknown } },
            { name: '检测异常', value: summary.error, itemStyle: { color: COLOR.error } }
        ];

        instance.setOption({
            tooltip: { trigger: 'item', formatter: '{b}：{c} 台（{d}%）' },
            legend: { bottom: 0, icon: 'circle', textStyle: { color: '#475569' } },
            series: [{
                type: 'pie',
                radius: ['50%', '72%'],
                center: ['50%', '44%'],
                avoidLabelOverlap: true,
                itemStyle: { borderColor: '#ffffff', borderWidth: 2, borderRadius: 7 },
                label: { show: false },
                data: data
            }],
            graphic: [{
                type: 'text',
                left: 'center',
                top: '37%',
                style: {
                    text: summary.deviceTotal + '\n台设备',
                    textAlign: 'center',
                    fill: '#0f172a',
                    fontSize: 20,
                    fontWeight: 700,
                    lineHeight: 26
                }
            }]
        }, true);
    }

    async function loadSummary() {
        const summary = await getJson('/api/dashboard/summary');
        setText('summary-device-total', summary.deviceTotal);
        setText('summary-online', summary.online);
        setText('summary-offline', summary.offline);
        setText('summary-active-alerts', summary.activeAlerts);
        setText('summary-monitor-records', summary.monitorRecords);
        setText('summary-generated-at', formatTime(summary.generatedAt));
        renderDeviceStatus(summary);
    }

    async function loadTrend() {
        const trend = await getJson('/api/dashboard/response-time-trend?hours=24');
        const instance = chart('response-time-chart');
        if (!instance) {
            return;
        }

        const hasData = trend.values.some(value => value != null);
        instance.setOption({
            tooltip: {
                trigger: 'axis',
                formatter: function (params) {
                    if (!params || params.length === 0) {
                        return '';
                    }
                    const item = params[0];
                    if (item == null || item.value == null) {
                        return (item ? item.axisValue : '') + '<br/>无在线检测数据';
                    }
                    return item.axisValue + '<br/>平均响应时间：' + item.value + ' ms';
                }
            },
            grid: { left: 48, right: 24, top: 28, bottom: 70 },
            xAxis: {
                type: 'category',
                data: trend.labels,
                boundaryGap: false,
                axisLabel: { interval: 3, rotate: 45, color: '#64748b', fontSize: 10 },
                axisLine: { lineStyle: { color: '#cbd5e1' } }
            },
            yAxis: {
                type: 'value',
                name: 'ms',
                min: 0,
                axisLabel: { color: '#64748b' },
                splitLine: { lineStyle: { color: '#e2e8f0' } }
            },
            series: [{
                name: '平均响应时间',
                type: 'line',
                data: trend.values,
                smooth: true,
                connectNulls: false,
                showSymbol: true,
                symbol: 'circle',
                symbolSize: 5,
                lineStyle: { width: 2, color: COLOR.blue },
                itemStyle: { color: COLOR.blue },
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(37, 99, 235, 0.25)' },
                        { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
                    ])
                }
            }],
            graphic: hasData ? [] : [{
                type: 'text',
                left: 'center',
                top: '46%',
                style: {
                    text: '最近 24 小时暂无在线检测数据',
                    fill: '#64748b',
                    fontSize: 14
                }
            }]
        }, true);
    }

    async function loadAlertDistribution() {
        const distribution = await getJson('/api/dashboard/alert-distribution');
        const instance = chart('alert-distribution-chart');
        if (!instance) {
            return;
        }

        const status = distribution.statusCounts;
        const data = [
            { name: '未恢复', value: status.OPEN || 0, itemStyle: { color: COLOR.open } },
            { name: '已确认', value: status.ACKNOWLEDGED || 0, itemStyle: { color: COLOR.acknowledged } },
            { name: '已恢复', value: status.RECOVERED || 0, itemStyle: { color: COLOR.recovered } }
        ];
        const total = data.reduce((sum, item) => sum + item.value, 0);

        instance.setOption({
            tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
            legend: { bottom: 0, icon: 'circle', textStyle: { color: '#475569' } },
            series: [{
                type: 'pie',
                radius: ['50%', '72%'],
                center: ['50%', '44%'],
                itemStyle: { borderColor: '#ffffff', borderWidth: 2, borderRadius: 7 },
                label: { show: false },
                data: data
            }],
            graphic: [{
                type: 'text',
                left: 'center',
                top: '37%',
                style: {
                    text: total + '\n条告警',
                    textAlign: 'center',
                    fill: total > 0 ? '#0f172a' : '#64748b',
                    fontSize: 20,
                    fontWeight: 700,
                    lineHeight: 26
                }
            }]
        }, true);
    }

    async function loadAll() {
        const button = document.getElementById('refresh-dashboard');
        if (button) {
            button.disabled = true;
            button.textContent = '刷新中...';
        }

        try {
            await Promise.all([loadSummary(), loadTrend(), loadAlertDistribution()]);
        } catch (error) {
            console.error('看板数据加载失败', error);
        } finally {
            if (button) {
                button.disabled = false;
                button.textContent = '刷新数据';
            }
        }
    }

    document.getElementById('refresh-dashboard')?.addEventListener('click', loadAll);

    window.addEventListener('resize', function () {
        Object.values(charts).forEach(function (instance) {
            instance.resize();
        });
    });

    loadAll();
})();
