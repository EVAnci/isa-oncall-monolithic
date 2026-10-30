import dayjs from 'dayjs/esm';

import { ITurnoDeGuardia, NewTurnoDeGuardia } from './turno-de-guardia.model';

export const sampleWithRequiredData: ITurnoDeGuardia = {
  id: 14445,
  desde: dayjs('2023-12-04T10:50'),
  hasta: dayjs('2023-12-04T08:12'),
};

export const sampleWithPartialData: ITurnoDeGuardia = {
  id: 6248,
  desde: dayjs('2023-12-03T22:58'),
  hasta: dayjs('2023-12-04T19:30'),
  esReemplazo: true,
  rol: 'PRIMARIO',
};

export const sampleWithFullData: ITurnoDeGuardia = {
  id: 17895,
  desde: dayjs('2023-12-04T14:21'),
  hasta: dayjs('2023-12-04T14:23'),
  esReemplazo: true,
  rol: 'PRIMARIO',
  nota: 'sanity zowie',
};

export const sampleWithNewData: NewTurnoDeGuardia = {
  desde: dayjs('2023-12-04T19:53'),
  hasta: dayjs('2023-12-04T05:38'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
