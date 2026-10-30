import { Injectable } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';

import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IEjecucionDeEscalamiento, NewEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IEjecucionDeEscalamiento for edit and NewEjecucionDeEscalamientoFormGroupInput for create.
 */
type EjecucionDeEscalamientoFormGroupInput = IEjecucionDeEscalamiento | PartialWithRequiredKeyOf<NewEjecucionDeEscalamiento>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IEjecucionDeEscalamiento | NewEjecucionDeEscalamiento> = Omit<T, 'ejecutadoEn' | 'proximoEn'> & {
  ejecutadoEn?: string | null;
  proximoEn?: string | null;
};

type EjecucionDeEscalamientoFormRawValue = FormValueOf<IEjecucionDeEscalamiento>;

type NewEjecucionDeEscalamientoFormRawValue = FormValueOf<NewEjecucionDeEscalamiento>;

type EjecucionDeEscalamientoFormDefaults = Pick<NewEjecucionDeEscalamiento, 'id' | 'ejecutadoEn' | 'proximoEn'>;

type EjecucionDeEscalamientoFormGroupContent = {
  id: FormControl<EjecucionDeEscalamientoFormRawValue['id'] | NewEjecucionDeEscalamiento['id']>;
  vuelta: FormControl<EjecucionDeEscalamientoFormRawValue['vuelta']>;
  nivel: FormControl<EjecucionDeEscalamientoFormRawValue['nivel']>;
  resultado: FormControl<EjecucionDeEscalamientoFormRawValue['resultado']>;
  ejecutadoEn: FormControl<EjecucionDeEscalamientoFormRawValue['ejecutadoEn']>;
  proximoEn: FormControl<EjecucionDeEscalamientoFormRawValue['proximoEn']>;
  detalle: FormControl<EjecucionDeEscalamientoFormRawValue['detalle']>;
  incidente: FormControl<EjecucionDeEscalamientoFormRawValue['incidente']>;
  paso: FormControl<EjecucionDeEscalamientoFormRawValue['paso']>;
  destinatario: FormControl<EjecucionDeEscalamientoFormRawValue['destinatario']>;
};

export type EjecucionDeEscalamientoFormGroup = FormGroup<EjecucionDeEscalamientoFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class EjecucionDeEscalamientoFormService {
  createEjecucionDeEscalamientoFormGroup(
    ejecucionDeEscalamiento?: EjecucionDeEscalamientoFormGroupInput,
  ): EjecucionDeEscalamientoFormGroup {
    const ejecucionDeEscalamientoRawValue = this.convertEjecucionDeEscalamientoToEjecucionDeEscalamientoRawValue({
      ...this.getFormDefaults(),
      ...(ejecucionDeEscalamiento ?? { id: null }),
    });

    return new FormGroup<EjecucionDeEscalamientoFormGroupContent>({
      id: new FormControl(
        { value: ejecucionDeEscalamientoRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      vuelta: new FormControl(ejecucionDeEscalamientoRawValue.vuelta, {
        validators: [Validators.required, Validators.min(0), Validators.max(5)],
      }),
      nivel: new FormControl(ejecucionDeEscalamientoRawValue.nivel, {
        validators: [Validators.required, Validators.min(0), Validators.max(10)],
      }),
      resultado: new FormControl(ejecucionDeEscalamientoRawValue.resultado, {
        validators: [Validators.required],
      }),
      ejecutadoEn: new FormControl(ejecucionDeEscalamientoRawValue.ejecutadoEn, {
        validators: [Validators.required],
      }),
      proximoEn: new FormControl(ejecucionDeEscalamientoRawValue.proximoEn),
      detalle: new FormControl(ejecucionDeEscalamientoRawValue.detalle, {
        validators: [Validators.maxLength(500)],
      }),
      incidente: new FormControl(ejecucionDeEscalamientoRawValue.incidente, {
        validators: [Validators.required],
      }),
      paso: new FormControl(ejecucionDeEscalamientoRawValue.paso),
      destinatario: new FormControl(ejecucionDeEscalamientoRawValue.destinatario),
    });
  }

  getEjecucionDeEscalamiento(form: EjecucionDeEscalamientoFormGroup): IEjecucionDeEscalamiento | NewEjecucionDeEscalamiento {
    return this.convertEjecucionDeEscalamientoRawValueToEjecucionDeEscalamiento(form.getRawValue());
  }

  resetForm(form: EjecucionDeEscalamientoFormGroup, ejecucionDeEscalamiento: EjecucionDeEscalamientoFormGroupInput): void {
    const ejecucionDeEscalamientoRawValue = this.convertEjecucionDeEscalamientoToEjecucionDeEscalamientoRawValue({
      ...this.getFormDefaults(),
      ...ejecucionDeEscalamiento,
    });
    form.reset({
      ...ejecucionDeEscalamientoRawValue,
      id: { value: ejecucionDeEscalamientoRawValue.id, disabled: true },
    });
  }

  private getFormDefaults(): EjecucionDeEscalamientoFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      ejecutadoEn: currentTime,
      proximoEn: currentTime,
    };
  }

  private convertEjecucionDeEscalamientoRawValueToEjecucionDeEscalamiento(
    rawEjecucionDeEscalamiento: EjecucionDeEscalamientoFormRawValue | NewEjecucionDeEscalamientoFormRawValue,
  ): IEjecucionDeEscalamiento | NewEjecucionDeEscalamiento {
    return {
      ...rawEjecucionDeEscalamiento,
      ejecutadoEn: dayjs(rawEjecucionDeEscalamiento.ejecutadoEn, DATE_TIME_FORMAT),
      proximoEn: dayjs(rawEjecucionDeEscalamiento.proximoEn, DATE_TIME_FORMAT),
    };
  }

  private convertEjecucionDeEscalamientoToEjecucionDeEscalamientoRawValue(
    ejecucionDeEscalamiento: IEjecucionDeEscalamiento | (Partial<NewEjecucionDeEscalamiento> & EjecucionDeEscalamientoFormDefaults),
  ): EjecucionDeEscalamientoFormRawValue | PartialWithRequiredKeyOf<NewEjecucionDeEscalamientoFormRawValue> {
    return {
      ...ejecucionDeEscalamiento,
      ejecutadoEn: ejecucionDeEscalamiento.ejecutadoEn ? ejecucionDeEscalamiento.ejecutadoEn.format(DATE_TIME_FORMAT) : undefined,
      proximoEn: ejecucionDeEscalamiento.proximoEn ? ejecucionDeEscalamiento.proximoEn.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
