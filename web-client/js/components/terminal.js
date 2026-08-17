const TerminalPanel = (() => {
  let term = null;
  let fitAddon = null;
  let panel = null;
  let terminalId = null;
  let _unsubscribe = null;

  function init() {
    panel = document.getElementById('panel-terminal');
    panel.innerHTML = `
      <div class="terminal-toolbar">
        <span style="font-size:0.75rem;color:var(--text-muted);flex:1">Shell Terminal</span>
        <button class="btn btn-sm btn-outline" onclick="TerminalPanel._clear()">Clear</button>
        <button class="btn btn-sm btn-outline" onclick="TerminalPanel._disconnect()">Disconnect</button>
      </div>
      <div class="terminal-container" id="terminal-container"></div>
    `;
  }

  function refresh() {
    _connect();
  }

  function _connect() {
    if (typeof window.Terminal !== 'function') {
      const container = document.getElementById('terminal-container');
      if (container) {
        container.innerHTML = '<p style="color:var(--text-muted);padding:16px;font-size:0.85rem;">Terminal library (xterm.js) is not loaded. Add xterm.js to use this panel.</p>';
      }
      return;
    }
    if (term) { term.dispose(); }

    const container = document.getElementById('terminal-container');
    terminalId = 't' + Date.now();

    term = new (window.Terminal)({
      cursorBlink: true,
      fontSize: 13,
      fontFamily: "'Cascadia Code', 'Fira Code', 'JetBrains Mono', monospace",
      theme: {
        background: '#0a0e17',
        foreground: '#e2e8f0',
        cursor: '#3b82f6',
        selectionBackground: '#1e3a5f'
      }
    });

    if (typeof FitAddon !== 'undefined') {
      fitAddon = new FitAddon.FitAddon();
      term.loadAddon(fitAddon);
    }

    term.open(container);

    if (fitAddon) {
      fitAddon.fit();
      window.addEventListener('resize', () => { if (fitAddon) fitAddon.fit(); });
    } else {
      term.resize(80, 24);
    }

    term.onData((data) => {
      API.send('terminal', { action: 'input', terminalId, data }, (resp, err) => {
        if (err && resp === null) return;
      });
    });

    API.send('terminal', { action: 'open', terminalId, cols: term.cols, rows: term.rows }, (data, err) => {
      if (err) { term.write('\r\n\x1b[31mFailed to open terminal\x1b[0m\r\n'); return; }
    });

    if (_unsubscribe) _unsubscribe();
    _unsubscribe = API.onMessage((msg) => {
      if (msg.type === 'event' && msg.payload) {
        const evType = msg.payload.type;
        if (evType === 'terminal_output') {
          let inner = msg.payload.data;
          if (typeof inner === 'string') {
            try { inner = JSON.parse(inner); } catch (e) {}
          }
          const tid = (typeof inner === 'object' && inner) ? inner.terminalId : null;
          const chunk = (typeof inner === 'object' && inner) ? inner.data : (typeof inner === 'string' ? inner : null);
          if (tid === terminalId && chunk && term) {
            term.write(chunk);
          }
        }
      }
    });
  }

  function _clear() {
    if (term) term.clear();
  }

  function _disconnect() {
    API.send('terminal', { action: 'close', terminalId });
    if (term) {
      term.write('\r\n\x1b[33mDisconnected\x1b[0m\r\n');
    }
  }

  function destroy() {
    if (_unsubscribe) { _unsubscribe(); _unsubscribe = null; }
    if (terminalId) {
      API.send('terminal', { action: 'close', terminalId });
    }
    if (term) { term.dispose(); term = null; }
  }

  return { init, refresh, destroy, _clear, _disconnect };
})();
