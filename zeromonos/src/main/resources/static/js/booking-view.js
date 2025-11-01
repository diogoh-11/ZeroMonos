class BookingViewer {
  constructor() {
    this.base = '/api/bookings';
    this.token = new URLSearchParams(window.location.search).get('token');
    this.setupElements();
    this.initialize();
  }

  setupElements() {
    this.searchForm = document.getElementById('search-form');
    this.tokenInput = document.getElementById('token');
    this.searchBtn = document.getElementById('search-btn');
    this.detailsSection = document.getElementById('booking-details');
    this.detailsContent = document.querySelector('.details-content');
    this.cancelBtn = document.getElementById('cancel-btn');
    this.messageContainer = document.getElementById('message');

    this.searchBtn.addEventListener('click', () => this.searchBooking());
    this.cancelBtn.addEventListener('click', () => this.cancelBooking());
  }

  async initialize() {
    if (this.token) {
      this.tokenInput.value = this.token;
      await this.searchBooking();
    }
  }

  async searchBooking() {
    const token = this.tokenInput.value.trim();
    if (!token) {
      this.showMessage('Digite um token válido', 'error');
      return;
    }

    try {
      const response = await fetch(`${this.base}/${token}`);
      if (!response.ok) {
        let errMsg = null;
        try {
          const errJson = await response.json();
          errMsg = errJson && errJson.message ? errJson.message : null;
        } catch (e) {}
        throw new Error(errMsg || 'Reserva não encontrada');
      }

      const booking = await response.json();
      this.displayBooking(booking);
    } catch (error) {
      this.showMessage(error.message, 'error');
      this.detailsSection.classList.add('hidden');
    }
  }

  displayBooking(booking) {
    this.detailsSection.classList.remove('hidden');
    this.detailsContent.innerHTML = `
      <div class="detail-row">
        <label>ID:</label>
        <span>${booking.id}</span>
      </div>
      <div class="detail-row">
        <label>Token:</label>
        <span>${booking.token}</span>
      </div>
      <div class="detail-row">
        <label>Município:</label>
        <span>${booking.municipalityName}</span>
      </div>
      <div class="detail-row">
        <label>Data:</label>
        <span>${new Date(booking.requestedDate).toLocaleDateString()}</span>
      </div>
      <div class="detail-row">
        <label>Horário:</label>
        <span>${this.formatTimeSlot(booking.timeSlot)}</span>
      </div>
      <div class="detail-row">
        <label>Status:</label>
        <span class="status-${booking.status.toLowerCase()}">${this.formatStatus(booking.status)}</span>
      </div>
      <div class="detail-row">
        <label>Descrição:</label>
        <span>${booking.description}</span>
      </div>
      <div class="detail-row">
        <label>Criado em:</label>
        <span>${this.formatDateTime(booking.createdAt)}</span>
      </div>
      <div class="detail-row">
        <label>Última atualização:</label>
        <span>${this.formatDateTime(booking.updatedAt)}</span>
      </div>
      ${this.renderHistory(booking.history)}
    `;
  }

  // Adicione estes métodos auxiliares à classe
  formatDateTime(timestamp) {
      if (!timestamp) return '-';
      const date = new Date(timestamp);
      return date.toLocaleString('pt-BR', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
      });
  }

  renderHistory(history) {
      if (!history || history.length === 0) return '';
      
      return `
        <div class="detail-row history-section">
          <label>Histórico:</label>
          <ul class="history-list">
            ${history.map(entry => `<li>${entry}</li>`).join('')}
          </ul>
        </div>
    `;
  }

  async cancelBooking() {
    if (!confirm('Tem certeza que deseja cancelar esta reserva?')) return;

    try {
      const token = this.tokenInput.value.trim();
      const response = await fetch(`${this.base}/${token}/cancel`, { method: 'PUT' });
      
      if (!response.ok) {
        let errMsg = null;
        try {
          const errJson = await response.json();
          errMsg = errJson && errJson.message ? errJson.message : null;
        } catch (e) {}
        throw new Error(errMsg || 'Não foi possível cancelar a reserva');
      }

      this.showMessage('Reserva cancelada com sucesso', 'success');
      await this.searchBooking();
    } catch (error) {
      this.showMessage(error.message, 'error');
    }
  }

  showMessage(text, type = 'info') {
    this.messageContainer.innerHTML = `
      <div class="message ${type}">
        <p>${text}</p>
      </div>
    `;
  }

  formatTimeSlot(slot) {
    const slots = {
      MORNING: 'Manhã',
      MIDDAY: 'Meio-dia',
      AFTERNOON: 'Tarde',
      EVENING: 'Fim de tarde',
      NIGHT: 'Noite'
    };
    return slots[slot] || slot;
  }

  formatStatus(status) {
    const statuses = {
      PENDING: 'Pendente',
      CONFIRMED: 'Confirmada',
      CANCELLED: 'Cancelada',
      COMPLETED: 'Concluída'
    };
    return statuses[status] || status;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  new BookingViewer();
});