import {
  default as makeWASocket,
  useMultiFileAuthState,
  DisconnectReason,
  fetchLatestBaileysVersion,
} from '@whiskeysockets/baileys';
import express from 'express';
import pino from 'pino';
import qrcode from 'qrcode-terminal';

const logger = pino({ level: process.env.LOG_LEVEL ?? 'info' });
const PORT = Number(process.env.PORT ?? 3000);
const AUTH_DIR = process.env.AUTH_DIR ?? './auth';

let sock = null;
let ready = false;

async function connect() {
  const { state, saveCreds } = await useMultiFileAuthState(AUTH_DIR);
  const { version } = await fetchLatestBaileysVersion();

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
      logger.info('Escaneie o QR Code abaixo no WhatsApp do número dono do canal:');
      qrcode.generate(qr, { small: true });
    }
    if (connection === 'open') {
      ready = true;
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

await connect();

const app = express();
app.use(express.json({ limit: '2mb' }));

app.get('/health', (req, res) => {
  res.json({ ready });
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

app.listen(PORT, () => logger.info(`Sidecar Baileys ouvindo em :${PORT}`));
