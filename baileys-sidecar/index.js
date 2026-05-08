import {
  default as makeWASocket,
  useMultiFileAuthState,
  DisconnectReason,
  fetchLatestBaileysVersion,
} from '@whiskeysockets/baileys';
import express from 'express';
import pino from 'pino';
import qrcodeTerminal from 'qrcode-terminal';
import QRCode from 'qrcode';

const logger = pino({ level: process.env.LOG_LEVEL ?? 'info' });
const PORT = Number(process.env.PORT ?? 3000);
const AUTH_DIR = process.env.AUTH_DIR ?? './auth';

let sock = null;
let ready = false;
let latestQR = null;

async function connect() {
  const { state, saveCreds } = await useMultiFileAuthState(AUTH_DIR);
  let version;
  try {
    ({ version } = await fetchLatestBaileysVersion());
  } catch (err) {
    logger.warn({ err: err.message }, 'fetchLatestBaileysVersion falhou — usando default');
  }

  sock = makeWASocket({
    version,
    auth: state,
    logger: logger.child({ module: 'baileys' }),
    printQRInTerminal: false,
    syncFullHistory: false,
    markOnlineOnConnect: false,
  });

  sock.ev.on('creds.update', saveCreds);

  sock.ev.on('connection.update', ({ connection, lastDisconnect, qr }) => {
    if (qr) {
      latestQR = qr;
      logger.info('Novo QR Code disponível em GET /qr (HTML) ou /qr.png (imagem).');
      qrcodeTerminal.generate(qr, { small: true });
    }
    if (connection === 'open') {
      ready = true;
      latestQR = null;
      logger.info('Conectado ao WhatsApp.');
    }
    if (connection === 'close') {
      ready = false;
      const code = lastDisconnect?.error?.output?.statusCode;
      const shouldReconnect = code !== DisconnectReason.loggedOut;
      logger.warn({ code }, `Conexão fechada. Reconectando: ${shouldReconnect}`);
      if (shouldReconnect) setTimeout(connect, 2000);
    }
  });
}

const app = express();
app.use(express.json({ limit: '2mb' }));

app.get('/health', (req, res) => {
  res.json({ ready });
});

app.get('/qr', async (req, res) => {
  if (ready) {
    return res.send('<h1 style="font-family:sans-serif;text-align:center;margin-top:40vh">✅ Já conectado ao WhatsApp.</h1>');
  }
  if (!latestQR) {
    return res.send('<h1 style="font-family:sans-serif;text-align:center;margin-top:40vh">⏳ Aguardando geração do QR... recarregue em alguns segundos.</h1>');
  }
  const dataUrl = await QRCode.toDataURL(latestQR, { width: 400, margin: 2 });
  res.send(`<!DOCTYPE html><html><head><meta http-equiv="refresh" content="20"><title>Baileys QR</title></head><body style="display:flex;flex-direction:column;justify-content:center;align-items:center;height:100vh;background:#111;color:#eee;font-family:sans-serif"><h2>Escaneie no WhatsApp do número dono do canal</h2><img src="${dataUrl}" alt="QR" /><p style="opacity:.6;font-size:14px">A página recarrega a cada 20s para acompanhar a rotação do QR.</p></body></html>`);
});

app.get('/qr.png', async (req, res) => {
  if (!latestQR) return res.status(404).send('no QR');
  const buf = await QRCode.toBuffer(latestQR, { width: 400, margin: 2 });
  res.type('png').send(buf);
});

app.post('/send-text', async (req, res) => {
  const { phone, message } = req.body ?? {};
  if (!phone || !message) return res.status(400).json({ error: 'phone and message required' });
  if (!ready) return res.status(503).json({ error: 'whatsapp not connected' });
  try {
    const result = await sock.sendMessage(phone, { text: message });
    res.json({ ok: true, messageId: result?.key?.id });
  } catch (err) {
    logger.error({ err }, 'send-text failed');
    res.status(500).json({ error: err.message });
  }
});

app.post('/send-image', async (req, res) => {
  const { phone, image, caption } = req.body ?? {};
  if (!phone || !image) return res.status(400).json({ error: 'phone and image required' });
  if (!ready) return res.status(503).json({ error: 'whatsapp not connected' });
  try {
    const result = await sock.sendMessage(phone, {
      image: { url: image },
      caption: caption ?? '',
    });
    res.json({ ok: true, messageId: result?.key?.id });
  } catch (err) {
    logger.error({ err }, 'send-image failed');
    res.status(500).json({ error: err.message });
  }
});

app.listen(PORT, () => {
  logger.info(`Sidecar Baileys ouvindo em :${PORT}`);
  connect().catch(err => logger.error({ err: err.message }, 'connect failed'));
});
