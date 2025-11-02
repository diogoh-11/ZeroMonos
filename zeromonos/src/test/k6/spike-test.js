import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 10 },   // Início tranquilo
    { duration: '10s', target: 200 },  // SPIKE! 
    { duration: '30s', target: 200 },  // Mantém carga alta
    { duration: '10s', target: 10 },   // Volta ao normal
    { duration: '10s', target: 0 },    // Finaliza
  ],
  thresholds: {
    http_req_duration: ['p(99)<2000'], // 99% < 2s (mais permissivo)
    http_req_failed: ['rate<0.2'],     // 80% de sucesso
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Teste principal: listar municípios (endpoint mais leve)
  const res = http.get(`${BASE_URL}/api/bookings/municipalities`);
  
  check(res, {
    'status 200': (r) => r.status === 200,
    'response time acceptable': (r) => r.timings.duration < 1500,
  });
  
  sleep(0.5);
}

export function setup() {
  console.log('\n' + '='.repeat(60));
  console.log('K6 Spike Test - Sudden Traffic Surge');
  console.log('='.repeat(60));
  console.log(`Target: ${BASE_URL}`);
  console.log('Scenario: 10 → 200 users in 10 seconds');
  console.log('='.repeat(60) + '\n');
}

export function teardown() {
  console.log('\n' + '='.repeat(60));
  console.log('K6 Spike Test Completed');
  console.log('='.repeat(60) + '\n');
}