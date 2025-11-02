import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '5s', target: 100 },   // Ramp-up rápido
    { duration: '5s', target: 300 },    // Stress máximo
    { duration: '5s', target: 0 },     // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.3'],
    errors: ['rate<0.3'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VALID_TIME_SLOTS = ['MORNING', 'MIDDAY', 'EVENING', 'NIGHT', 'ANYTIME'];

function getValidFutureDate() {
  let futureDate = new Date();
  let daysToAdd = Math.floor(Math.random() * 30) + 1;
  futureDate.setDate(futureDate.getDate() + daysToAdd);
  
  while (futureDate.getDay() === 0) {
    futureDate.setDate(futureDate.getDate() + 1);
  }
  
  return futureDate.toISOString().split('T')[0];
}

export function setup() {
  console.log('K6 Stress Test - High Load in 50 seconds');
  console.log(`Target: ${BASE_URL}`);
  
  const res = http.get(`${BASE_URL}/api/bookings/municipalities`);
  let municipalities = ['Lisboa', 'Porto', 'Braga', 'Coimbra', 'Aveiro'];
  
  if (res.status === 200) {
    try {
      municipalities = JSON.parse(res.body);
      console.log(`Found ${municipalities.length} municipalities`);
    } catch (e) {
      municipalities = ['Lisboa', 'Porto', 'Braga', 'Coimbra', 'Aveiro'];
    }
  }
  
  console.log('Load profile: 100 -> 300 users in 40 seconds');
  
  return { municipalities: municipalities };
}

export default function (data) {
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // 70% criar bookings, 30% listar municípios
  if (Math.random() < 0.7) {
    const municipality = data.municipalities[Math.floor(Math.random() * data.municipalities.length)];
    const timeSlot = VALID_TIME_SLOTS[Math.floor(Math.random() * VALID_TIME_SLOTS.length)];
    const validDate = getValidFutureDate();
    
    const payload = JSON.stringify({
      municipalityName: municipality,
      requestedDate: validDate,
      timeSlot: timeSlot,
      description: `Stress VU${__VU}`,
    });
    
    const res = http.post(`${BASE_URL}/api/bookings`, payload, params);
    
    check(res, {
      'booking created': (r) => r.status === 200,
    }) || errorRate.add(1);
    
  } else {
    const res = http.get(`${BASE_URL}/api/bookings/municipalities`);
    
    check(res, {
      'municipalities OK': (r) => r.status === 200,
    }) || errorRate.add(1);
  }

  sleep(0.5);
}

export function teardown() {
  console.log('K6 Stress Test Completed');
}