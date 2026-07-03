import './style.css'

document.addEventListener('DOMContentLoaded', () => {
  // ─────────────────────────────────────────────────────────────────────────
  // API Endpoints — relative paths via Vite proxy (no hardcoded localhost)
  // ─────────────────────────────────────────────────────────────────────────
  const API_PORTFOLIO      = '/api/v1/portfolio';
  const API_STRATEGIES     = '/api/v1/strategies';
  const API_STRATEGIES_ALL = '/api/v1/strategies/all';
  const API_ENGINE_TRIGGER = '/api/v1/engine/trigger';
  const API_ENGINE_STATUS  = '/api/v1/engine/status';

  // ─────────────────────────────────────────────────────────────────────────
  // State
  // ─────────────────────────────────────────────────────────────────────────
  let backendOnline = false;
  let previousNlv = null;
  let currentTargets = [];
  let pollingInterval = null;

  // ─────────────────────────────────────────────────────────────────────────
  // Navigation
  // ─────────────────────────────────────────────────────────────────────────
  const navBtns = document.querySelectorAll('.nav-btn');
  const views = document.querySelectorAll('.view');

  navBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      navBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const targetId = btn.getAttribute('data-target');
      views.forEach(view => {
        view.classList.toggle('hidden', view.id !== targetId);
      });
      document.querySelector('.top-bar h1').textContent = btn.textContent.trim();
    });
  });

  // ─────────────────────────────────────────────────────────────────────────
  // Connection Status Indicator
  // ─────────────────────────────────────────────────────────────────────────
  function setConnectionStatus(online) {
    backendOnline = online;
    const dot = document.querySelector('.dot');
    const label = document.querySelector('.status-indicator');
    if (online) {
      dot.classList.add('pulse');
      dot.style.background = '#22c55e';
      label.childNodes[label.childNodes.length - 1].textContent = ' System Online';
    } else {
      dot.classList.remove('pulse');
      dot.style.background = '#ef4444';
      dot.style.boxShadow = 'none';
      label.childNodes[label.childNodes.length - 1].textContent = ' Backend Offline';
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Toast Notification System
  // ─────────────────────────────────────────────────────────────────────────
  function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success'
      ? '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>'
      : '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>';
    toast.innerHTML = `${icon}<span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
      toast.classList.add('fade-out');
      toast.addEventListener('animationend', () => toast.remove());
    }, 4000);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Loading State Helpers
  // ─────────────────────────────────────────────────────────────────────────
  function setLoading(elementId, loading) {
    const el = document.getElementById(elementId);
    if (!el) return;
    if (loading) {
      el.dataset.original = el.textContent;
      el.innerHTML = '<span class="spinner"></span>';
    } else {
      el.textContent = el.dataset.original || '---';
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Portfolio Fetch — No Mock Fallback
  // ─────────────────────────────────────────────────────────────────────────
  async function fetchPortfolio() {
    try {
      const response = await fetch(API_PORTFOLIO);
      if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      const data = await response.json();
      setConnectionStatus(true);
      renderPortfolio(data);
    } catch (error) {
      setConnectionStatus(false);
      renderPortfolioError(error.message);
    }
  }

  function renderPortfolio(data) {
    // NLV with real trend
    const nlv = data.netLiquidationValue?.amount;
    if (nlv !== undefined && nlv !== null) {
      const nlvFormatted = `$${Number(nlv).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
      document.getElementById('nlv-value').textContent = nlvFormatted;

      // Compute real NLV trend
      if (previousNlv !== null && previousNlv > 0) {
        const changePct = ((Number(nlv) - previousNlv) / previousNlv) * 100;
        const trendEl = document.querySelector('.metric-card .trend');
        if (trendEl) {
          trendEl.textContent = (changePct >= 0 ? '+' : '') + changePct.toFixed(2) + '%';
          trendEl.className = `trend ${changePct >= 0 ? 'positive' : 'negative'}`;
        }
      }
      previousNlv = Number(nlv);
    } else {
      document.getElementById('nlv-value').textContent = 'N/A';
    }

    // Last updated timestamp
    if (data.lastUpdated) {
      const updatedEl = document.getElementById('positions-updated');
      if (updatedEl) {
        updatedEl.textContent = `Updated ${new Date(data.lastUpdated).toLocaleTimeString()}`;
      }
    }

    // Positions table
    const positions = data.positions || [];
    document.getElementById('positions-count').textContent = positions.length;
    const tbody = document.querySelector('#positions-table tbody');
    tbody.innerHTML = '';

    if (positions.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color: var(--text-muted); padding: 20px;">No open positions</td></tr>';
    } else {
      positions.forEach(pos => {
        const isLong = Number(pos.quantity) >= 0;
        const qty = Math.abs(Number(pos.quantity));
        const avgPrice = pos.averageCost?.amount ?? 0;
        const mktPrice = pos.marketPrice?.amount ?? 0;
        const pnl = pos.unrealizedPnL?.amount ?? 0;
        const pnlClass = pnl >= 0 ? 'positive' : 'negative';
        const row = document.createElement('tr');
        row.innerHTML = `
          <td><strong>${pos.symbol}</strong></td>
          <td class="${isLong ? 'positive' : 'negative'}">${isLong ? 'LONG' : 'SHORT'}</td>
          <td>${qty}</td>
          <td>$${Number(avgPrice).toFixed(2)}</td>
          <td>$${Number(mktPrice).toFixed(2)}</td>
          <td class="${pnlClass}">${pnl >= 0 ? '+' : ''}$${Number(pnl).toFixed(2)}</td>
        `;
        tbody.appendChild(row);
      });
    }

    // Additional metrics
    const cash = data.totalCashValue?.amount;
    const buyingPower = data.buyingPower?.amount;
    const unrealizedPnl = data.unrealizedPnL?.amount;

    const cashEl = document.getElementById('cash-value');
    if (cashEl && cash !== undefined) cashEl.textContent = `$${Number(cash).toLocaleString(undefined, {minimumFractionDigits: 2})}`;

    const bpEl = document.getElementById('buying-power-value');
    if (bpEl && buyingPower !== undefined) bpEl.textContent = `$${Number(buyingPower).toLocaleString(undefined, {minimumFractionDigits: 2})}`;

    const pnlEl = document.getElementById('unrealized-pnl-value');
    if (pnlEl && unrealizedPnl !== undefined) {
      pnlEl.textContent = `${unrealizedPnl >= 0 ? '+' : ''}$${Number(unrealizedPnl).toLocaleString(undefined, {minimumFractionDigits: 2})}`;
      pnlEl.className = `value ${unrealizedPnl >= 0 ? 'positive' : 'negative'}`;
    }
  }

  function renderPortfolioError(message) {
    document.getElementById('nlv-value').textContent = '—';
    document.getElementById('positions-count').textContent = '—';
    const tbody = document.querySelector('#positions-table tbody');
    tbody.innerHTML = '';

    const row = document.createElement('tr');
    const cell = document.createElement('td');
    const retryButton = document.createElement('button');

    cell.colSpan = 6;
    cell.className = 'table-error';
    cell.textContent = `Backend unavailable: ${message}`;

    retryButton.type = 'button';
    retryButton.className = 'inline-retry-btn';
    retryButton.textContent = 'Retry';
    retryButton.addEventListener('click', fetchPortfolio);

    cell.appendChild(retryButton);
    row.appendChild(cell);
    tbody.appendChild(row);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Strategies Fetch — No Mock Fallback
  // ─────────────────────────────────────────────────────────────────────────
  async function fetchStrategies() {
    try {
      const response = await fetch(API_STRATEGIES_ALL);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      setConnectionStatus(true);
      renderStrategies(Array.isArray(data) ? data : []);
    } catch (error) {
      setConnectionStatus(false);
      renderStrategiesError(error.message);
    }
  }

  function renderStrategiesError(message) {
    const listContainer = document.getElementById('strategies-list');
    listContainer.innerHTML = '';

    const errorPanel = document.createElement('div');
    const retryButton = document.createElement('button');

    errorPanel.className = 'strategy-error';
    errorPanel.textContent = `Could not load strategies: ${message}`;

    retryButton.type = 'button';
    retryButton.className = 'inline-retry-btn';
    retryButton.textContent = 'Retry';
    retryButton.addEventListener('click', fetchStrategies);

    errorPanel.appendChild(retryButton);
    listContainer.appendChild(errorPanel);
  }

  function getStateBadgeColor(state) {
    const colors = {
      'IDLE': '#6b7280',
      'BUY_TRIGGERED': '#f59e0b',
      'BUY_EXECUTING': '#3b82f6',
      'BUY_COMPLETED': '#22c55e',
      'SELL_TRIGGERED': '#f97316',
      'SELL_EXECUTING': '#8b5cf6',
      'SELL_COMPLETED': '#10b981',
      'ERROR': '#ef4444',
      'RECOVERY': '#f59e0b',
    };
    return colors[state] || '#6b7280';
  }

  function renderStrategies(strategies) {
    const listContainer = document.getElementById('strategies-list');
    listContainer.innerHTML = '';

    if (strategies.length === 0) {
      listContainer.innerHTML = '<p style="color: var(--text-muted); padding: 10px;">No strategies found. Create one below.</p>';
      return;
    }

    strategies.forEach(strat => {
      const state = strat.state || 'IDLE';
      const stateColor = getStateBadgeColor(state);
      const isEnabled = strat.enabled;

      const buyThreshold = strat.buyThreshold != null ? `$${Number(strat.buyThreshold).toLocaleString()}` : '—';
      const sellThreshold = strat.sellThreshold != null ? `$${Number(strat.sellThreshold).toLocaleString()}` : '—';

      const targets = strat.targets && strat.targets.length > 0
        ? strat.targets.map(t => `<span class="target-badge">${t.symbol} ×${t.quantity}</span>`).join(' ')
        : '<span style="color: var(--text-muted); font-size: 0.8rem;">No basket targets</span>';

      const item = document.createElement('div');
      item.className = 'strategy-item';
      item.dataset.id = strat.id;
      item.innerHTML = `
        <div class="strategy-info">
          <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap;">
            <h4>${strat.name || 'Unnamed Strategy'}</h4>
            <span style="background: ${stateColor}22; color: ${stateColor}; border: 1px solid ${stateColor}44; padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">${state}</span>
            <span style="background: ${isEnabled ? '#22c55e22' : '#6b728022'}; color: ${isEnabled ? '#22c55e' : '#9ca3af'}; border: 1px solid ${isEnabled ? '#22c55e44' : '#6b728044'}; padding: 2px 8px; border-radius: 12px; font-size: 0.75rem;">${isEnabled ? 'ENABLED' : 'DISABLED'}</span>
          </div>
          ${strat.description ? `<p style="color: var(--text-muted); font-size: 0.85rem; margin-top: 4px;">${strat.description}</p>` : ''}
          <div style="margin-top: 8px; display: flex; flex-wrap: wrap; gap: 6px;">${targets}</div>
          <div style="margin-top: 8px; font-size: 0.8rem; color: var(--text-muted); display: flex; gap: 16px; flex-wrap: wrap;">
            <span>🎯 Buy ≤ ${buyThreshold}</span>
            <span>📈 Sell ≥ ${sellThreshold}</span>
            <span>⚡ ${strat.executionMode || 'PAPER'}</span>
            <span>🛡 ${strat.riskProfile || '—'}</span>
            <span>⏱ ${strat.cooldownMinutes || 0}m cooldown</span>
          </div>
        </div>
        <div style="display: flex; flex-direction: column; gap: 6px; align-items: flex-end; min-width: 100px;">
          <button class="btn-toggle-strategy" data-id="${strat.id}" data-enabled="${isEnabled}"
            style="padding: 5px 10px; font-size: 0.78rem; background: ${isEnabled ? 'rgba(239,68,68,0.15)' : 'rgba(34,197,94,0.15)'}; border: 1px solid ${isEnabled ? '#ef444466' : '#22c55e66'}; color: ${isEnabled ? '#ef4444' : '#22c55e'}; border-radius: 6px; cursor: pointer; white-space: nowrap;">
            ${isEnabled ? 'Disable' : 'Enable'}
          </button>
          <button class="btn-delete-strategy" data-id="${strat.id}" data-name="${strat.name}"
            style="padding: 5px 10px; font-size: 0.78rem; background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); color: #ef4444; border-radius: 6px; cursor: pointer;">
            Delete
          </button>
        </div>
      `;
      listContainer.appendChild(item);
    });

    // Attach toggle listeners
    document.querySelectorAll('.btn-toggle-strategy').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        const isEnabled = btn.dataset.enabled === 'true';
        const action = isEnabled ? 'disable' : 'enable';
        try {
          const res = await fetch(`${API_STRATEGIES}/${id}/${action}`, { method: 'PUT' });
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          showToast(`Strategy ${action}d successfully`);
          fetchStrategies();
        } catch (err) {
          showToast(`Failed to ${action} strategy: ${err.message}`, 'error');
        }
      });
    });

    // Attach delete listeners
    document.querySelectorAll('.btn-delete-strategy').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        const name = btn.dataset.name;
        if (!confirm(`Are you sure you want to delete strategy "${name}"? This cannot be undone.`)) return;
        try {
          const res = await fetch(`${API_STRATEGIES}/${id}`, { method: 'DELETE' });
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          showToast('Strategy deleted successfully');
          fetchStrategies();
        } catch (err) {
          showToast('Failed to delete strategy: ' + err.message, 'error');
        }
      });
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // System Controls
  // ─────────────────────────────────────────────────────────────────────────
  const btnTrigger = document.getElementById('btn-trigger-pipeline');
  const btnStatus = document.getElementById('btn-engine-status');
  const statusDisplay = document.getElementById('engine-status-display');

  if (btnTrigger) {
    btnTrigger.addEventListener('click', async () => {
      btnTrigger.disabled = true;
      btnTrigger.textContent = 'Running...';
      statusDisplay.textContent = 'Triggering pipeline...';

      try {
        const response = await fetch(API_ENGINE_TRIGGER, { method: 'POST' });
        if (!response.ok) {
          const err = await response.json().catch(() => ({ message: response.statusText }));
          throw new Error(err.message || 'Failed to trigger pipeline');
        }
        const data = await response.json();
        showToast(data.message || 'Trading pipeline triggered successfully', 'success');
        statusDisplay.textContent = `✅ ${data.message || 'Pipeline triggered'}`;
        // Refresh strategies to see updated states
        setTimeout(() => { fetchStrategies(); fetchPortfolio(); }, 1500);
      } catch (error) {
        showToast('Error triggering pipeline: ' + error.message, 'error');
        statusDisplay.textContent = '❌ Failed: ' + error.message;
      } finally {
        setTimeout(() => {
          btnTrigger.disabled = false;
          btnTrigger.textContent = 'Run Trading Pipeline';
        }, 2000);
      }
    });
  }

  if (btnStatus) {
    btnStatus.addEventListener('click', async () => {
      try {
        const response = await fetch(API_ENGINE_STATUS);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const data = await response.json();
        statusDisplay.textContent = `Status: ${data.status?.toUpperCase()} — ${data.message}`;
      } catch (error) {
        statusDisplay.textContent = '⚠ Status: Unable to reach engine';
      }
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Basket Target Builder
  // ─────────────────────────────────────────────────────────────────────────
  const targetBtn = document.getElementById('btn-add-target');
  const testBasketBtn = document.getElementById('btn-test-basket');
  const targetContainer = document.getElementById('basket-targets-container');
  const testBasketTargets = [
    { symbol: 'SNDK', assetClass: 'STOCK', quantity: 1 },
    { symbol: 'META', assetClass: 'STOCK', quantity: 1 },
    { symbol: 'NVDA', assetClass: 'STOCK', quantity: 1 },
  ];

  function renderTargetRow(sym, cls, qty, index) {
    const row = document.createElement('div');
    row.className = 'target-row';
    row.dataset.index = index;
    row.style.cssText = 'display: flex; justify-content: space-between; align-items: center; background: rgba(255,255,255,0.05); padding: 8px 12px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.08);';
    row.innerHTML = `
      <span><strong>${sym}</strong> <span style="color: var(--text-muted);">(${cls})</span></span>
      <span>×${qty} shares</span>
      <button type="button" data-index="${index}" style="background: rgba(239,68,68,0.15); border: 1px solid rgba(239,68,68,0.3); color: #ef4444; border-radius: 4px; padding: 3px 8px; cursor: pointer; font-size: 0.8rem;">Remove</button>
    `;
    row.querySelector('button').addEventListener('click', () => {
      currentTargets.splice(index, 1);
      rebuildTargetContainer();
    });
    return row;
  }

  function rebuildTargetContainer() {
    targetContainer.innerHTML = '';
    currentTargets.forEach((t, i) => targetContainer.appendChild(renderTargetRow(t.symbol, t.assetClass, t.quantity, i)));
  }

  if (targetBtn) {
    targetBtn.addEventListener('click', () => {
      const sym = document.getElementById('target-symbol').value.trim().toUpperCase();
      const cls = document.getElementById('target-class').value;
      const qty = parseFloat(document.getElementById('target-quantity').value);

      if (!sym) { showToast('Please enter a symbol (e.g. AAPL)', 'error'); return; }
      if (isNaN(qty) || qty <= 0) { showToast('Please enter a valid positive quantity', 'error'); return; }
      if (currentTargets.some(t => t.symbol === sym)) { showToast(`${sym} is already in the basket`, 'error'); return; }

      currentTargets.push({ symbol: sym, assetClass: cls, quantity: qty });
      rebuildTargetContainer();

      document.getElementById('target-symbol').value = '';
      document.getElementById('target-quantity').value = '';
      document.getElementById('target-symbol').focus();
    });
  }

  if (testBasketBtn) {
    testBasketBtn.addEventListener('click', () => {
      currentTargets = testBasketTargets.map(target => ({ ...target }));
      rebuildTargetContainer();
      showToast('Test basket added');
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Strategy Creation Form
  // ─────────────────────────────────────────────────────────────────────────
  const form = document.getElementById('create-strategy-form');
  const submitBtn = form?.querySelector('button[type="submit"]');

  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();

      const formData = new FormData(form);
      const buyThreshold = parseFloat(formData.get('buyThreshold') || '0');
      const sellThreshold = parseFloat(formData.get('sellThreshold') || '0');

      // Client-side validation
      if (buyThreshold > 0 && sellThreshold > 0 && sellThreshold <= buyThreshold) {
        showToast('Sell threshold must be greater than buy threshold', 'error');
        return;
      }

      if (buyThreshold > 0 && currentTargets.length === 0) {
        showToast('Add at least one basket target before creating the strategy', 'error');
        return;
      }

      const strategyName = (formData.get('name') || '').trim()
        || `Buy ${buyThreshold} / Sell ${sellThreshold} Basket`;
      const newStrategy = {
        name: strategyName,
        description: (formData.get('description') || '').trim()
          || `Buy basket at or below ${buyThreshold}; sell at or above ${sellThreshold}`,
        riskProfile: formData.get('riskProfile'),
        executionMode: formData.get('executionMode'),
        cooldownMinutes: parseInt(formData.get('cooldownMinutes'), 10) || 5,
        priority: parseInt(formData.get('priority'), 10) || 1,
        buyThreshold: buyThreshold || null,
        sellThreshold: sellThreshold || null,
        targets: currentTargets.map(target => ({ ...target })),
        enabled: true
      };

      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Deploying...';
      }

      try {
        const response = await fetch(API_STRATEGIES, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(newStrategy)
        });

        if (!response.ok) {
          const err = await response.json().catch(() => ({ message: response.statusText }));
          throw new Error(err.message || `HTTP ${response.status}`);
        }

        showToast(`Strategy "${newStrategy.name}" created successfully`, 'success');
        form.reset();
        currentTargets = [];
        rebuildTargetContainer();
        document.getElementById('buyThreshold').value = '1000.00';
        document.getElementById('sellThreshold').value = '1200.00';
        fetchStrategies();

      } catch (error) {
        showToast('Failed to create strategy: ' + error.message, 'error');
        console.error('Strategy creation error:', error);
      } finally {
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.textContent = 'Create Strategy';
        }
      }
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Initial Load + Polling (5 seconds)
  // ─────────────────────────────────────────────────────────────────────────
  fetchPortfolio();
  fetchStrategies();

  pollingInterval = setInterval(() => {
    fetchPortfolio();
    fetchStrategies();
  }, 5000);
});
