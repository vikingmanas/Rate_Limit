/**
 * Dashboard auto-refresh logic and Chart.js integration.
 * Polls /api/metrics/* every 5 seconds and updates the UI.
 */
(function () {
    'use strict';

    // ── Chart configuration ──────────────────────────────────────
    const chartColors = {
        indigo: '#6366f1',
        violet: '#8b5cf6',
        emerald: '#10b981',
        rose: '#f43f5e',
        cyan: '#06b6d4',
        amber: '#f59e0b'
    };

    // Pie chart: Allowed vs Blocked
    const pieCtx = document.getElementById('pieChart').getContext('2d');
    const pieChart = new Chart(pieCtx, {
        type: 'doughnut',
        data: {
            labels: ['Allowed', 'Blocked'],
            datasets: [{
                data: [0, 0],
                backgroundColor: [chartColors.emerald, chartColors.rose],
                borderColor: 'transparent',
                borderWidth: 0,
                hoverOffset: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        color: '#94a3b8',
                        padding: 16,
                        font: { size: 12, weight: '600' },
                        usePointStyle: true,
                        pointStyleWidth: 12
                    }
                }
            }
        }
    });

    // Line chart: Requests per minute
    const lineCtx = document.getElementById('lineChart').getContext('2d');
    const lineChart = new Chart(lineCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Allowed',
                    data: [],
                    borderColor: chartColors.emerald,
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointHoverRadius: 6,
                    borderWidth: 2
                },
                {
                    label: 'Blocked',
                    data: [],
                    borderColor: chartColors.rose,
                    backgroundColor: 'rgba(244, 63, 94, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointHoverRadius: 6,
                    borderWidth: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { intersect: false, mode: 'index' },
            scales: {
                x: {
                    ticks: { color: '#64748b', font: { size: 10 }, maxTicksLimit: 12 },
                    grid: { color: 'rgba(99,102,241,0.06)' }
                },
                y: {
                    beginAtZero: true,
                    ticks: { color: '#64748b', font: { size: 11 } },
                    grid: { color: 'rgba(99,102,241,0.06)' }
                }
            },
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        color: '#94a3b8',
                        font: { size: 11, weight: '600' },
                        usePointStyle: true,
                        pointStyleWidth: 10
                    }
                }
            }
        }
    });

    // ── DOM elements ──────────────────────────────────────────────
    const els = {
        totalRequests: document.getElementById('totalRequests'),
        allowedRequests: document.getElementById('allowedRequests'),
        blockedRequests: document.getElementById('blockedRequests'),
        blockRate: document.getElementById('blockRate'),
        redisStatus: document.getElementById('redisStatus'),
        algorithmBadge: document.getElementById('algorithmBadge'),
        topUsersBody: document.getElementById('topUsersBody'),
        lastUpdate: document.getElementById('lastUpdate')
    };

    // ── Animate number changes ───────────────────────────────────
    function animateValue(el, newValue) {
        const old = el.textContent;
        el.textContent = newValue;
        if (old !== String(newValue)) {
            el.classList.add('updated');
            setTimeout(() => el.classList.remove('updated'), 600);
        }
    }

    // ── Fetch and update ─────────────────────────────────────────
    async function fetchJson(url) {
        try {
            const res = await fetch(url);
            return res.ok ? await res.json() : null;
        } catch (e) {
            console.warn('Fetch failed:', url, e);
            return null;
        }
    }

    async function refresh() {
        // Summary
        const summary = await fetchJson('/api/metrics/summary');
        if (summary) {
            animateValue(els.totalRequests, summary.totalRequests.toLocaleString());
            animateValue(els.allowedRequests, summary.allowedRequests.toLocaleString());
            animateValue(els.blockedRequests, summary.blockedRequests.toLocaleString());
            animateValue(els.blockRate, summary.blockRatePercent + '%');

            // Pie chart
            pieChart.data.datasets[0].data = [summary.allowedRequests, summary.blockedRequests];
            pieChart.update('none');

            // Redis status
            if (summary.redisAvailable) {
                els.redisStatus.classList.remove('offline');
                els.redisStatus.querySelector('.label').textContent = 'Redis Online';
            } else {
                els.redisStatus.classList.add('offline');
                els.redisStatus.querySelector('.label').textContent = 'Redis Offline';
            }

            // Algorithm
            els.algorithmBadge.textContent = summary.activeAlgorithm;
        }

        // Timeline
        const timeline = await fetchJson('/api/metrics/timeline');
        if (timeline && timeline.length > 0) {
            const labels = timeline.map(t => t.minute.substring(11)); // "13:08"
            const allowed = timeline.map(t => t.allowed);
            const blocked = timeline.map(t => t.blocked);

            lineChart.data.labels = labels;
            lineChart.data.datasets[0].data = allowed;
            lineChart.data.datasets[1].data = blocked;
            lineChart.update('none');
        }

        // Top users
        const topUsers = await fetchJson('/api/metrics/top-users');
        if (topUsers && topUsers.length > 0) {
            let html = '';
            topUsers.forEach((user, i) => {
                const rate = user.requests > 0
                    ? ((user.blocked / user.requests) * 100).toFixed(1)
                    : '0.0';
                const statusClass = rate > 50 ? 'critical' : rate > 20 ? 'warning' : 'healthy';
                const statusLabel = rate > 50 ? 'High Block' : rate > 20 ? 'Moderate' : 'Healthy';
                html += `<tr>
                    <td>#${i + 1}</td>
                    <td><code>${user.key}</code></td>
                    <td>${user.requests.toLocaleString()}</td>
                    <td>${user.blocked.toLocaleString()}</td>
                    <td>${rate}%</td>
                    <td><span class="status-pill ${statusClass}">${statusLabel}</span></td>
                </tr>`;
            });
            els.topUsersBody.innerHTML = html;
        }

        // Timestamp
        els.lastUpdate.textContent = 'Last update: ' + new Date().toLocaleTimeString();
    }

    // Initial load + 5s interval
    refresh();
    setInterval(refresh, 5000);
})();
