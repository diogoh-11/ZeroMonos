class BookingForm {
  constructor() {
    this.base = '/api/bookings';
    this.form = document.getElementById('booking-form');
    this.municipalityInput = document.getElementById('municipality');
    this.suggestionsDropdown = document.getElementById('suggestions-dropdown');
    this.messageContainer = document.getElementById('form-msg');
    this.municipalities = [];
    this.selectedIndex = -1;
    
    this.initializeForm();
  }

  async initializeForm() {
    await this.loadMunicipalities();
    this.form.addEventListener('submit', (e) => this.handleSubmit(e));
    this.setupAutocomplete();
  }

  async loadMunicipalities() {
    try {
      const response = await fetch(`${this.base}/municipalities`);
      if (!response.ok) throw new Error('Erro ao carregar municípios');
      
      this.municipalities = await response.json();
    } catch (error) {
      this.showError('Erro ao carregar municípios: ' + error.message);
    }
  }

  setupAutocomplete() {
    // Mostra sugestões ao digitar
    this.municipalityInput.addEventListener('input', (e) => {
      const value = e.target.value.trim();
      this.selectedIndex = -1;
      
      if (value.length === 0) {
        this.hideSuggestions();
        return;
      }
      
      this.showSuggestions(value);
    });

    // Navegação por teclado
    this.municipalityInput.addEventListener('keydown', (e) => {
      const items = this.suggestionsDropdown.querySelectorAll('.suggestion-item');
      
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        this.selectedIndex = Math.min(this.selectedIndex + 1, items.length - 1);
        this.updateSelectedItem(items);
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
        this.updateSelectedItem(items);
      } else if (e.key === 'Enter' && this.selectedIndex >= 0) {
        e.preventDefault();
        items[this.selectedIndex].click();
      } else if (e.key === 'Escape') {
        this.hideSuggestions();
      }
    });

    // Fecha ao clicar fora
    document.addEventListener('click', (e) => {
      if (!this.municipalityInput.contains(e.target) && !this.suggestionsDropdown.contains(e.target)) {
        this.hideSuggestions();
      }
    });
  }

  showSuggestions(query) {
    const filtered = this.municipalities.filter(m => 
      m.toLowerCase().includes(query.toLowerCase())
    );

    if (filtered.length === 0) {
      this.suggestionsDropdown.innerHTML = '<div class="no-suggestions">Nenhum município encontrado</div>';
      this.suggestionsDropdown.classList.add('active');
      return;
    }

    this.suggestionsDropdown.innerHTML = filtered
      .slice(0, 10) // Limita a 10 sugestões
      .map(municipality => 
        `<div class="suggestion-item" data-value="${this.escapeHtml(municipality)}">
          ${this.highlightMatch(municipality, query)}
         </div>`
      )
      .join('');

    // Adiciona evento de clique
    this.suggestionsDropdown.querySelectorAll('.suggestion-item').forEach(item => {
      item.addEventListener('click', () => {
        this.municipalityInput.value = item.dataset.value;
        this.hideSuggestions();
        this.municipalityInput.focus();
      });
    });

    this.suggestionsDropdown.classList.add('active');
  }

  updateSelectedItem(items) {
    items.forEach((item, index) => {
      if (index === this.selectedIndex) {
        item.classList.add('selected');
        item.scrollIntoView({ block: 'nearest' });
      } else {
        item.classList.remove('selected');
      }
    });
  }

  hideSuggestions() {
    this.suggestionsDropdown.classList.remove('active');
    this.selectedIndex = -1;
  }

  highlightMatch(text, query) {
    const regex = new RegExp(`(${query})`, 'gi');
    return this.escapeHtml(text).replace(regex, '<strong>$1</strong>');
  }

  async handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(this.form);
    const data = Object.fromEntries(formData);

    try {
      const response = await fetch(this.base, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      if (!response.ok) {
          // tenta extrair JSON { message: '...' } do servidor
          let errMsg = null;
          try {
            const errJson = await response.json();
            errMsg = errJson && errJson.message ? errJson.message : null;
          } catch (e) {
            // ignore parse error
          }
          const text = errMsg || await response.text() || `Erro ${response.status}`;
          throw new Error(text);
      }

      const result = await response.json();
      this.showSuccess(result.token);
      this.form.reset();
    } catch (error) {
      this.showError(error.message);
    }
  }

  showSuccess(token) {
    this.messageContainer.innerHTML = `
      <div class="message success">
        <p>Reserva criada com sucesso!</p>
        <p>Token: <strong>${this.escapeHtml(token)}</strong></p>
        <p><a href="/booking-view.html?token=${encodeURIComponent(token)}">Ver detalhes</a></p>
      </div>
    `;
  }

  showError(message) {
    this.messageContainer.innerHTML = `
      <div class="message error">
        <p>${this.escapeHtml(message)}</p>
      </div>
    `;
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}

// Inicializar quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
  new BookingForm();
});