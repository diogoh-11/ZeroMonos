import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.1'],
    http_req_failed: ['rate<0.1'],
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
  console.log('K6 Performance Test - Booking API');
  console.log(`Target: ${BASE_URL}`);
  
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  const res = http.get(`${BASE_URL}/api/bookings/municipalities`, params);
  let municipalities = [];
  
  if (res.status === 200) {
    try {
      municipalities = JSON.parse(res.body);
      console.log(`Found ${municipalities.length} municipalities`);
    } catch (e) {
      console.error('Failed to parse municipalities');
      municipalities = ['Lisboa', 'Porto', 'Braga', 'Coimbra', 'Aveiro'];
    }
  } else {
    console.warn('Using default municipalities');
    municipalities = ['Lisboa', 'Porto', 'Braga', 'Coimbra', 'Aveiro'];
  }
  
  console.log('Load profile: 10 -> 50 -> 100 -> 0 users');
  
  return { municipalities: municipalities };
}

export default function (data) {
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  if (!data || !data.municipalities || data.municipalities.length === 0) {
    console.error('No municipalities available');
    errorRate.add(1);
    return;
  }

  let municipalitiesRes = http.get(`${BASE_URL}/api/bookings/municipalities`);
  
  check(municipalitiesRes, {
    'municipalities status 200': (r) => r.status === 200,
    'municipalities response time OK': (r) => r.timings.duration < 200,
  }) || errorRate.add(1);

  sleep(1);

  const randomMunicipality = data.municipalities[Math.floor(Math.random() * data.municipalities.length)];
  const randomTimeSlot = VALID_TIME_SLOTS[Math.floor(Math.random() * VALID_TIME_SLOTS.length)];
  const validDate = getValidFutureDate();

  const bookingPayload = JSON.stringify({
    municipalityName: randomMunicipality,
    requestedDate: validDate,
    timeSlot: randomTimeSlot,
    description: `K6 Performance Test - VU${__VU} Iter${__ITER}`,
  });

  let createRes = http.post(`${BASE_URL}/api/bookings`, bookingPayload, params);
  let bookingToken = null;

  const createCheck = check(createRes, {
    'create booking status 200': (r) => r.status === 200,
    'create booking response time OK': (r) => r.timings.duration < 500,
    'create booking returns token': (r) => {
      try {
        const body = JSON.parse(r.body);
        bookingToken = body.token;
        return bookingToken != null && bookingToken.length > 0;
      } catch (e) {
        return false;
      }
    },
  });

  if (!createCheck) {
    errorRate.add(1);
  }

  sleep(1);

  if (bookingToken) {
    let getRes = http.get(`${BASE_URL}/api/bookings/${bookingToken}`);
    
    check(getRes, {
      'get booking status 200': (r) => r.status === 200,
      'get booking response time OK': (r) => r.timings.duration < 300,
      'get booking correct data': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.token === bookingToken;
        } catch (e) {
          return false;
        }
      },
    }) || errorRate.add(1);

    sleep(1);

    if (Math.random() < 0.3) {
      let cancelRes = http.put(`${BASE_URL}/api/bookings/${bookingToken}/cancel`);
      
      check(cancelRes, {
        'cancel booking successful': (r) => r.status === 200 || r.status === 204,
        'cancel booking response time OK': (r) => r.timings.duration < 400,
      }) || errorRate.add(1);
    }
  }

  sleep(2);
}

export function teardown(data) {
  console.log('K6 Performance Test Completed');
  console.log(`Total municipalities used: ${data.municipalities.length}`);
}