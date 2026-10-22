const fs = require('fs');
const path = require('path');

const dir = __dirname;
const html = fs.readFileSync(path.join(dir, 'index.html'), 'utf8');
const js = fs.readFileSync(path.join(dir, 'app.js'), 'utf8');
const dataJs = fs.readFileSync(path.join(dir, 'data.js'), 'utf8');
const sessionJs = fs.readFileSync(path.join(dir, 'session.js'), 'utf8');
const apiJs = fs.readFileSync(path.join(dir, 'api.js'), 'utf8');
const allJs = [js, dataJs, sessionJs, apiJs].join('\n');

const problems = [];

// 1. Inline handlers in HTML must exist as window.* in JS
const handlerRe = /on(?:click|change|input|submit|keydown)="\s*([A-Za-z_$][\w$]*)\s*\(/g;
const handlers = new Set();
let m;
while ((m = handlerRe.exec(html))) handlers.add(m[1]);

const defined = new Set();
const defRe = /window\.([A-Za-z_$][\w$]*)\s*=/g;
while ((m = defRe.exec(allJs))) defined.add(m[1]);
// functions declared at top level inside the IIFE
const fnRe = /function\s+([A-Za-z_$][\w$]*)\s*\(/g;
while ((m = fnRe.exec(allJs))) defined.add(m[1]);
// const arrow / function expressions
const constFnRe = /(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*(?:async\s+)?(?:function|\()/g;
while ((m = constFnRe.exec(allJs))) defined.add(m[1]);

for (const h of handlers) {
  if (!defined.has(h)) problems.push('HTML handler not defined in JS: ' + h);
}

// 2. getElementById targets must exist in HTML
const htmlIds = new Set();
const idRe = /\bid="([^"]+)"/g;
while ((m = idRe.exec(html))) htmlIds.add(m[1]);

const gidRe = /getElementById\(\s*'([^']+)'\s*\)/g;
const missingIds = new Set();
while ((m = gidRe.exec(allJs))) {
  if (!htmlIds.has(m[1])) missingIds.add(m[1]);
}
for (const id of missingIds) problems.push('getElementById target missing in HTML: #' + id);

// 3. querySelector('#id') targets
const qsRe = /querySelector(?:All)?\(\s*'#([A-Za-z_][\w-]*)'/g;
const missingQs = new Set();
while ((m = qsRe.exec(allJs))) {
  if (!htmlIds.has(m[1])) missingQs.add(m[1]);
}
for (const id of missingQs) problems.push('querySelector target missing in HTML: #' + id);

// 4. Api.* calls must exist on the Api client
const apiMethods = new Set();
const apiDefRe = /\b(get|post|put|patch|del|request|discoverBase):\s*(?:function|discoverBase|[A-Za-z_$][\w$]*)/g;
while ((m = apiDefRe.exec(apiJs))) apiMethods.add(m[1]);
apiMethods.add('ApiError');
apiMethods.add('baseUrl');
apiMethods.add('setBaseUrl');
const usedApi = new Set();
const usedApiRe = /Api\.([A-Za-z_$][\w$]*)/g;
while ((m = usedApiRe.exec(allJs))) usedApi.add(m[1]);
for (const u of usedApi) {
  if (!apiMethods.has(u)) problems.push('Api.' + u + ' is not defined on the API client');
}

// 5. Data.* calls must exist
const dataMethods = new Set();
const dataDefRe = /(?:async\s+)?([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*\{\s*$/gm;
const dataObj = dataJs.slice(dataJs.indexOf('const Data = {'));
const dataMemberRe = /^\s{4}(?:async\s+)?([A-Za-z_$][\w$]*)\s*[:(]/gm;
while ((m = dataMemberRe.exec(dataObj))) dataMethods.add(m[1]);
const usedData = new Set();
const usedDataRe = /Data\.([A-Za-z_$][\w$]*)/g;
while ((m = usedDataRe.exec(allJs))) usedData.add(m[1]);
for (const u of usedData) {
  if (!dataMethods.has(u)) problems.push('Data.' + u + ' is not defined in data.js');
}

// 6. Session.* calls must exist
const sessionObj = sessionJs.slice(sessionJs.indexOf('const Session = {'));
const sessionMethods = new Set();
const sessMemberRe = /^\s{4}(?:get\s+)?([A-Za-z_$][\w$]*)\s*[:(]/gm;
while ((m = sessMemberRe.exec(sessionObj))) sessionMethods.add(m[1]);
const usedSession = new Set();
const usedSessionRe = /Session\.([A-Za-z_$][\w$]*)/g;
while ((m = usedSessionRe.exec(allJs))) usedSession.add(m[1]);
for (const u of usedSession) {
  if (!sessionMethods.has(u)) problems.push('Session.' + u + ' is not defined in session.js');
}

if (problems.length === 0) {
  console.log('Wiring OK - ' + handlers.size + ' HTML handlers, ' + htmlIds.size + ' element ids checked.');
} else {
  console.log('PROBLEMS (' + problems.length + '):');
  problems.forEach(p => console.log(' - ' + p));
  process.exitCode = 1;
}
