document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  const errorMessage = params.get('message') || 'Ocorreu um erro inesperado.';
  document.getElementById('error-details').textContent = errorMessage;
});