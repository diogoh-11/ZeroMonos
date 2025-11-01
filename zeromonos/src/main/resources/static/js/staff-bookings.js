class StaffBookings {
  constructor() {
    this.base = '/api/staff/bookings';
    this.muniBase = '/api/bookings/municipalities';
    this.filterEl = document.getElementById('municipality-filter');
    this.filterBtn = document.getElementById('filter-btn');
    this.tbody = document.getElementById('bookings-tbody');
    this.msg = document.getElementById('msg');

    this.init();
  }

  async init() {
    await this.loadMunicipalities();
    this.filterBtn.addEventListener('click', () => this.loadBookings());
    await this.loadBookings();
  }

  async loadMunicipalities() {
    try {
      const res = await fetch('/api/bookings/municipalities');
      if (!res.ok) throw new Error('Não foi possível carregar municípios');
      const list = await res.json();
      this.populateMunicipalities(list);
    } catch (e) {
      this.showMsg(e.message, 'error');
    }
  }

  populateMunicipalities(list) {
    // valor 'todas' será interpretado pelo backend como listar tudo
    this.filterEl.innerHTML = '<option value="todas">Todas</option>' +
      list.map(m => `<option value="${this.escape(m)}">${this.escape(m)}</option>`).join('');
  }

  async loadBookings() {
  let municipality = this.filterEl.value;
  if (!municipality || municipality.trim() === '') municipality = 'todas';
  const qs = `?municipality=${encodeURIComponent(municipality)}`;
    try {
      const res = await fetch(`${this.base}${qs}`);
      if (!res.ok) throw new Error('Erro ao carregar reservas');
      const list = await res.json();
      this.renderList(list);
    } catch (e) {
      this.showMsg(e.message, 'error');
    }
  }

  renderList(list) {
    if (!list || list.length === 0) {
      this.tbody.innerHTML = '<tr><td colspan="6">Sem reservas</td></tr>';
      return;
    }

    this.tbody.innerHTML = list.map(b => `
      <tr>
        <td>${this.escape(b.token)}</td>
        <td>${this.escape(b.municipalityName)}</td>
        <td>${this.escape(b.requestedDate || '')}</td>
        <td>${this.escape(b.timeSlot || '')}</td>
        <td>${this.escape(b.status || '')}</td>
        <td>
          <select data-token="${this.escape(b.token)}" class="status-select">
            <option value="RECEIVED">Received</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In progress</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
          <button class="btn btn-primary btn-update" data-token="${this.escape(b.token)}">Atualizar</button>
        </td>
      </tr>
    `).join('');

    // colocar valores selecionados e listeners
    this.tbody.querySelectorAll('.status-select').forEach(sel => {
      const token = sel.getAttribute('data-token');
      const row = list.find(r => r.token === token);
      if (row) sel.value = row.status;
    });

    this.tbody.querySelectorAll('.btn-update').forEach(btn => {
      btn.addEventListener('click', (ev) => this.updateStatus(ev));
    });
  }

  async updateStatus(ev) {
    const token = ev.currentTarget.getAttribute('data-token');
    const select = this.tbody.querySelector(`select[data-token="${token}"]`);
    const status = select.value;
    try {
      const res = await fetch(`${this.base}/${encodeURIComponent(token)}/status?status=${encodeURIComponent(status)}`, {
        method: 'PATCH'
      });
      if (!res.ok) {
        let err = 'Erro ao atualizar status';
        try { const j = await res.json(); err = j.message || err; } catch (e) {}
        throw new Error(err);
      }
      const updated = await res.json();
      this.showMsg('Status atualizado', 'success');
      await this.loadBookings();
    } catch (e) {
      this.showMsg(e.message, 'error');
    }
  }

  showMsg(text, type='info') {
    this.msg.innerHTML = `<div class="message ${type}">${this.escape(text)}</div>`;
    setTimeout(() => { this.msg.innerHTML = ''; }, 4000);
  }

  escape(s){ return (s==null)?'':String(s).replace(/[&<>"]+/g, c=>({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c])); }
}

document.addEventListener('DOMContentLoaded', () => new StaffBookings());
