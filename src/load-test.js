import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

const users = new SharedArray('users', function () {
    return open('./job_portal_10000_users.csv')
        .split('\n')
        .slice(1)
        .filter(line => line.trim() !== '')
        .map(line => {
            const [email, password] = line.split(',');
            return { email, password };
        });
});

export const options = {
   stages: [
    { duration: '30s', target: 500 },
    { duration: '5m', target: 500 },
    { duration: '30s', target: 0 }]
};

export default function () {

    const user = users[Math.floor(Math.random() * users.length)];

    const payload =
        `username=${encodeURIComponent(user.email.trim())}` +
        `&password=${encodeURIComponent(user.password.trim())}`;

    const params = {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
    };

    const response = http.post(
        'http://localhost/login',
        payload,
        params
    );

    check(response, {
        'login succeeded': (r) =>
            r.status === 200 || r.status === 302,

        'response time < 2s': (r) =>
            r.timings.duration < 2000
    });

    sleep(Math.random() * 1.5);
}

// MUST BE OUTSIDE default function
export function handleSummary(data) {
    return {
        "summary.html": htmlReport(data),
        stdout: textSummary(data, {
            indent: " ",
            enableColors: true,
        }),
    };
}