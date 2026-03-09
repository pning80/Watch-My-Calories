const express = require('express');
const cors = require('cors');

const app = express();
app.set('trust proxy', 1);
app.use(cors());

module.exports = { app };
