import dayjs from 'dayjs/esm';

import { IEjecucionDeEscalamiento, NewEjecucionDeEscalamiento } from './ejecucion-de-escalamiento.model';

export const sampleWithRequiredData: IEjecucionDeEscalamiento = {
  id: 9489,
  vuelta: 3,
  nivel: 10,
  resultado: 'NOTIFICADO',
  ejecutadoEn: dayjs('2026-10-23T06:49'),
};

export const sampleWithPartialData: IEjecucionDeEscalamiento = {
  id: 24741,
  vuelta: 3,
  nivel: 4,
  resultado: 'SIN_DESTINATARIO',
  ejecutadoEn: dayjs('2026-10-23T14:36'),
  proximoEn: dayjs('2026-10-23T09:51'),
  detalle: 'loyally curiously monocle',
};

export const sampleWithFullData: IEjecucionDeEscalamiento = {
  id: 22601,
  vuelta: 0,
  nivel: 2,
  resultado: 'NOTIFICADO',
  ejecutadoEn: dayjs('2026-10-23T21:21'),
  proximoEn: dayjs('2026-10-23T12:34'),
  detalle: 'pfft about',
};

export const sampleWithNewData: NewEjecucionDeEscalamiento = {
  vuelta: 5,
  nivel: 4,
  resultado: 'SIN_POLITICA',
  ejecutadoEn: dayjs('2026-10-23T20:39'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
