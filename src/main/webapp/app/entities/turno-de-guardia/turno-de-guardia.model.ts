import dayjs from 'dayjs/esm';

import { RolDeGuardia } from 'app/entities/enumerations/rol-de-guardia.model';
import { IRotacion } from 'app/entities/rotacion/rotacion.model';
import { IUser } from 'app/entities/user/user.model';

export interface ITurnoDeGuardia {
  id: number;
  desde?: dayjs.Dayjs | null;
  hasta?: dayjs.Dayjs | null;
  esReemplazo?: boolean | null;
  rol?: keyof typeof RolDeGuardia | null;
  nota?: string | null;
  rotacion?: Pick<IRotacion, 'id' | 'nombre'> | null;
  responsable?: Pick<IUser, 'id' | 'login'> | null;
}

export type NewTurnoDeGuardia = Omit<ITurnoDeGuardia, 'id'> & { id: null };
