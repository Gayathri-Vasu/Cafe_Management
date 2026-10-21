/*
 * Loads backend/.env (if present) and then starts Spring Boot via the Maven
 * wrapper. Values already present in the real environment take precedence.
 * Run with: npm start
 */
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const envFile = path.join(__dirname, '.env');

if (fs.existsSync(envFile)) {
  for (const line of fs.readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq === -1) continue;
    const key = trimmed.slice(0, eq).trim();
    let value = trimmed.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) process.env[key] = value;
  }
  console.log(`Loaded environment from ${envFile}`);
} else {
  console.log('No .env file found - using system environment variables / defaults');
}

const wrapper = process.platform === 'win32' ? 'mvnw.cmd' : './mvnw';
const child = spawn(wrapper, ['spring-boot:run'], {
  stdio: 'inherit',
  shell: true,
  cwd: __dirname,
});
child.on('exit', (code) => process.exit(code === null ? 1 : code));
