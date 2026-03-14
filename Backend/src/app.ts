import express from 'express';
import cors from 'cors';

export const app = express();
app.set('trust proxy', 1);
app.use(cors());
