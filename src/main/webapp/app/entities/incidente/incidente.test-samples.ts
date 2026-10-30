import dayjs from 'dayjs/esm';

import { IIncidente, NewIncidente } from './incidente.model';

export const sampleWithRequiredData: IIncidente = {
  id: 10165,
  titulo: 'to but aha',
  severidad: 'SEV3',
  estado: 'ABIERTO',
  detectadoEn: dayjs('2023-12-04T06:33'),
};

export const sampleWithPartialData: IIncidente = {
  id: 4347,
  titulo: 'ew whoa gadzooks',
  severidad: 'SEV1',
  estado: 'MITIGADO',
  detectadoEn: dayjs('2023-12-04T21:25'),
  reconocidoEn: dayjs('2023-12-04T16:25'),
  mitigadoEn: dayjs('2023-12-04T14:04'),
  usuariosAfectados: 15738,
  silenciadoHasta: dayjs('2023-12-04T08:24'),
};

export const sampleWithFullData: IIncidente = {
  id: 14089,
  titulo: 'embossing gosh pace',
  descripcion: 'eyeliner',
  severidad: 'SEV4',
  estado: 'ABIERTO',
  detectadoEn: dayjs('2023-12-04T22:15'),
  reconocidoEn: dayjs('2023-12-04T12:09'),
  mitigadoEn: dayjs('2023-12-04T15:17'),
  resueltoEn: dayjs('2023-12-04T06:38'),
  usuariosAfectados: 32687,
  cumplioObjetivo: false,
  silenciadoHasta: dayjs('2023-12-04T09:15'),
};

export const sampleWithNewData: NewIncidente = {
  titulo: 'crest deplore',
  severidad: 'SEV3',
  estado: 'ABIERTO',
  detectadoEn: dayjs('2023-12-04T18:11'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
