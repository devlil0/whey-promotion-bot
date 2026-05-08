FROM node:20-alpine

WORKDIR /app

RUN apk add --no-cache git python3 make g++

COPY package.json ./
RUN npm install --omit=dev

COPY index.js ./

ENV AUTH_DIR=/app/auth
EXPOSE 3000

CMD ["node", "index.js"]
