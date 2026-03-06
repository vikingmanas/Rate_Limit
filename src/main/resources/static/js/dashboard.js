/**
 * Dashboard — Interactive controls, auto-refresh, and Chart.js.
 */
(function () {
    'use strict';

    // ── Chart Colors ──────────────────────────────────────────────
    const C = {
        indigo: '#6366f1', violet: '#a855f7', emerald: '#10b981',
        rose: '#f43f5e', cyan: '#06b6d4', amber: '#f59e0b'
    };

    // ── Pie Chart ─────────────────────────────────────────────────
    const pieChart = new Chart(document.getElementById('pieChart').getContext('2d'), {
        type: 'doughnut',
        data: {
            labels: ['Allowed', 'Blocked'],
            datasets: [{
                data: [0, 0],
                backgroundColor: [C.emerald, C.rose],
                borderColor: 'transparent',
                hoverOffset: 10
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false, cutout: '68%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#94a3b8', padding: 14, font: { size: 12, weight: '600' }, usePointStyle: true }
                }
            },
            animation: { animateRotate: true, duration: 600 }
        }
    });

    // ── Line Chart ────────────────────────────────────────────────
    const lineChart = new Chart(document.getElementById('lineChart').getContext('2d'), {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: 'Allowed', data: [],
                    borderColor: C.emerald, backgroundColor: 'rgba(16,185,129,0.08)',
                    fill: true, tension: 0.4, pointRadius: 3, pointHoverRadius: 7, borderWidth: 2.5
                },
                {
                    label: 'Blocked', data: [],
                    borderColor: C.rose, backgroundColor: 'rgba(244,63,94,0.08)',
                    fill: true, tension: 0.4, pointRadius: 3, pointHoverRadius: 7, borderWidth: 2.5
                }
            ]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            interaction: { intersect: false, mode: 'index' },
            scales: {
                x: { ticks: { color: '#64748b', font: { size: 10 }, maxTicksLimit: 10 }, grid: { color: 'rgba(99,102,241,0.05)' } },
                y: { beginAtZero: true, ticks: { color: '#64748b', font: { size: 11 } }, grid: { color: 'rgba(99,102,241,0.05)' } }
            },
            plugins: {
                legend: { position: 'top', labels: { color: '#94a3b8', font: { size: 11, weight: '600' }, usePointStyle: true } }
            },
            animation: { duration: 400 }
        }
    });

    // ── DOM refs ───────────────────────────────────────────────────
    const $ = id => document.getElementById(id);
    const els = {
        total: $('totalRequests'), allowed: $('allowedRequests'),
        blocked: $('blockedRequests'), rate: $('blockRate'),
        redis: $('redisStatus'), algo: $('algorithmBadge'),
        users: $('topUsersBody'), lastUpdate: $('lastUpdate')
    };

    // ── Utilities ─────────────────────────────────────────────────
    function animateValue(el, val) {
        const old = el.textContent;
        el.textContent = val;
        if (old !== String(val)) { el.classList.add('updated'); setTimeout(() => el.classList.remove('updated'), 500); }
    }

    async function fetchJson(url, opts) {
        try { const r = await fetch(url, opts); return r.ok ? await r.json() : { _status: r.status }; }
        catch (e) { return null; }
    }

    // ── Toast Notifications ───────────────────────────────────────
    window.showToast = function (msg, type = 'info') {
        const icon = type === 'success' ? '✓' : type === 'error' ? '✗' : 'ℹ';
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span>${icon}</span> ${msg}`;
        toast.onclick = () => toast.remove();
        $('toastContainer').appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    };

    // ── Send Single Request ───────────────────────────────────────
    window.sendSingleRequest = async function () {
        const key = $('apiKeySelect').value;
        const endpoint = $('endpointSelect').value;
        const box = $('responseBox');
        const btn = $('sendRequestBtn');

        btn.disabled = true;
        box.textContent = '⏳ Sending…';
        box.className = 'response-box';

        const headers = {};
        if (key) headers['X-API-Key'] = key;

        try {
            const res = await fetch(endpoint, { headers });
            const data = await res.json();
            const pretty = JSON.stringify(data, null, 2);

            if (res.ok) {
                box.textContent = `✅ ${res.status} OK\n${pretty}`;
                box.className = 'response-box success';
                showToast('Request allowed', 'success');
            } else {
                box.textContent = `🚫 ${res.status} Rate Limited\n${pretty}`;
                box.className = 'response-box error';
                showToast('Request blocked — 429 Too Many Requests', 'error');
            }
        } catch (e) {
            box.textContent = `❌ Error: ${e.message}`;
            box.className = 'response-box error';
        }

        btn.disabled = false;
        setTimeout(refresh, 500);
    };

    // ── Burst Simulator ───────────────────────────────────────────
    window.startBurst = async function () {
        const count = parseInt($('burstCount').value);
        const delay = parseInt($('burstDelay').value);
        const key = $('apiKeySelect').value;
        const endpoint = $('endpointSelect').value;
        const btn = $('burstBtn');

        btn.disabled = true;
        btn.innerHTML = '⏳ Running…';

        let allowed = 0, blocked = 0;

        for (let i = 0; i < count; i++) {
            const headers = {};
            if (key) headers['X-API-Key'] = key;

            try {
                const res = await fetch(endpoint, { headers });
                if (res.ok) allowed++;
                else blocked++;
            } catch { blocked++; }

            const progress = ((i + 1) / count) * 100;
            $('progressFill').style.width = progress + '%';
            $('burstAllowed').textContent = '✓ ' + allowed;
            $('burstBlocked').textContent = '✗ ' + blocked;
            $('burstPercent').textContent = Math.round(progress) + '%';

            if (delay > 0) await new Promise(r => setTimeout(r, delay));
        }

        showToast(`Burst complete: ${allowed} allowed, ${blocked} blocked`, blocked > 0 ? 'error' : 'success');

        btn.disabled = false;
        btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg> Simulate Burst';

        setTimeout(refresh, 500);
    };

    // ── Algorithm Switcher ────────────────────────────────────────
    window.switchAlgorithm = async function (algo) {
        const res = await fetch(`/admin/config/algorithm?algorithm=${algo}`, { method: 'PUT' });
        if (res.ok) {
            document.querySelectorAll('.algo-btn').forEach(b => {
                b.classList.toggle('active', b.dataset.algo === algo);
            });
            els.algo.textContent = algo;
            showToast(`Algorithm switched to ${algo.replace(/_/g, ' ')}`, 'success');
        } else {
            showToast('Failed to switch algorithm', 'error');
        }
    };

    // ── Auto-refresh ──────────────────────────────────────────────
    async function refresh() {
        // Summary
        const s = await fetchJson('/api/metrics/summary');
        if (s && !s._status) {
            animateValue(els.total, s.totalRequests.toLocaleString());
            animateValue(els.allowed, s.allowedRequests.toLocaleString());
            animateValue(els.blocked, s.blockedRequests.toLocaleString());
            animateValue(els.rate, s.blockRatePercent + '%');

            pieChart.data.datasets[0].data = [s.allowedRequests, s.blockedRequests];
            pieChart.update('none');

            els.redis.classList.toggle('offline', !s.redisAvailable);
            els.redis.querySelector('.label').textContent = s.redisAvailable ? 'Redis Online' : 'Redis Offline';
            els.algo.textContent = s.activeAlgorithm;

            // Sync algo buttons
            document.querySelectorAll('.algo-btn').forEach(b => {
                b.classList.toggle('active', b.dataset.algo === s.activeAlgorithm);
            });
        }

        // Timeline
        const tl = await fetchJson('/api/metrics/timeline');
        if (tl && tl.length) {
            lineChart.data.labels = tl.map(t => t.minute.substring(11));
            lineChart.data.datasets[0].data = tl.map(t => t.allowed);
            lineChart.data.datasets[1].data = tl.map(t => t.blocked);
            lineChart.update('none');
        }

        // Top users
        const users = await fetchJson('/api/metrics/top-users');
        if (users && users.length) {
            els.users.innerHTML = users.map((u, i) => {
                const rate = u.requests > 0 ? ((u.blocked / u.requests) * 100).toFixed(1) : '0.0';
                const cls = rate > 50 ? 'critical' : rate > 20 ? 'warning' : 'healthy';
                const label = rate > 50 ? 'High Block' : rate > 20 ? 'Moderate' : 'Healthy';
                return `<tr>
                    <td>#${i + 1}</td>
                    <td><code>${u.key}</code></td>
                    <td>${u.requests.toLocaleString()}</td>
                    <td>${u.blocked.toLocaleString()}</td>
                    <td>${rate}%</td>
                    <td><span class="status-pill ${cls}">${label}</span></td>
                </tr>`;
            }).join('');
        }

        els.lastUpdate.textContent = 'Last update: ' + new Date().toLocaleTimeString();
    }

    // ── Init ──────────────────────────────────────────────────────
    refresh();
    setInterval(refresh, 3000);

    // Load plan rules from admin API
    (async function loadRules() {
        const rules = await fetchJson('/admin/rules');
        if (rules) {
            const container = $('planRules');
            container.innerHTML = '';
            for (const [plan, rule] of Object.entries(rules)) {
                const cls = plan === 'FREE' ? 'free' : plan === 'PREMIUM' ? 'premium' : 'admin';
                const chip = document.createElement('div');
                chip.className = `plan-chip ${cls}`;
                chip.textContent = `${plan}: ${rule.maxRequests} req/${rule.windowSeconds}s`;
                if (rule.burstMultiplier > 1) chip.textContent += ` (${rule.burstMultiplier}× burst)`;
                container.appendChild(chip);
            }
        }
    })();
})();
