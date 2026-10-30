import { beforeEach, describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../ejecucion-de-escalamiento.test-samples';

import { EjecucionDeEscalamientoFormService } from './ejecucion-de-escalamiento-form.service';

describe('EjecucionDeEscalamiento Form Service', () => {
  let service: EjecucionDeEscalamientoFormService;

  beforeEach(() => {
    service = TestBed.inject(EjecucionDeEscalamientoFormService);
  });

  describe('Service methods', () => {
    describe('createEjecucionDeEscalamientoFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createEjecucionDeEscalamientoFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            vuelta: expect.any(Object),
            nivel: expect.any(Object),
            resultado: expect.any(Object),
            ejecutadoEn: expect.any(Object),
            proximoEn: expect.any(Object),
            detalle: expect.any(Object),
            incidente: expect.any(Object),
            paso: expect.any(Object),
            destinatario: expect.any(Object),
          }),
        );
      });

      it('passing IEjecucionDeEscalamiento should create a new form with FormGroup', () => {
        const formGroup = service.createEjecucionDeEscalamientoFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            vuelta: expect.any(Object),
            nivel: expect.any(Object),
            resultado: expect.any(Object),
            ejecutadoEn: expect.any(Object),
            proximoEn: expect.any(Object),
            detalle: expect.any(Object),
            incidente: expect.any(Object),
            paso: expect.any(Object),
            destinatario: expect.any(Object),
          }),
        );
      });
    });

    describe('getEjecucionDeEscalamiento', () => {
      it('should return NewEjecucionDeEscalamiento for default EjecucionDeEscalamiento initial value', () => {
        const formGroup = service.createEjecucionDeEscalamientoFormGroup(sampleWithNewData);

        const ejecucionDeEscalamiento = service.getEjecucionDeEscalamiento(formGroup);

        expect(ejecucionDeEscalamiento).toMatchObject(sampleWithNewData);
      });

      it('should return NewEjecucionDeEscalamiento for empty EjecucionDeEscalamiento initial value', () => {
        const formGroup = service.createEjecucionDeEscalamientoFormGroup();

        const ejecucionDeEscalamiento = service.getEjecucionDeEscalamiento(formGroup);

        expect(ejecucionDeEscalamiento).toMatchObject({});
      });

      it('should return IEjecucionDeEscalamiento', () => {
        const formGroup = service.createEjecucionDeEscalamientoFormGroup(sampleWithRequiredData);

        const ejecucionDeEscalamiento = service.getEjecucionDeEscalamiento(formGroup);

        expect(ejecucionDeEscalamiento).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IEjecucionDeEscalamiento should not enable id FormControl', () => {
        const formGroup = service.createEjecucionDeEscalamientoFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewEjecucionDeEscalamiento should disable id FormControl', () => {
        const formGroup = service.createEjecucionDeEscalamientoFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
