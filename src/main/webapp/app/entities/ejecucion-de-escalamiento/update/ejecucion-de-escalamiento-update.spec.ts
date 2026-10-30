import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { HttpResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { provideTranslateService } from '@ngx-translate/core';
import { Subject, from, of } from 'rxjs';

import { IIncidente } from 'app/entities/incidente/incidente.model';
import { IncidenteService } from 'app/entities/incidente/service/incidente.service';
import { IPasoEscalamiento } from 'app/entities/paso-escalamiento/paso-escalamiento.model';
import { PasoEscalamientoService } from 'app/entities/paso-escalamiento/service/paso-escalamiento.service';
import { UserService } from 'app/entities/user/service/user.service';
import { IUser } from 'app/entities/user/user.model';
import { IEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';
import { EjecucionDeEscalamientoService } from '../service/ejecucion-de-escalamiento.service';

import { EjecucionDeEscalamientoFormService } from './ejecucion-de-escalamiento-form.service';
import { EjecucionDeEscalamientoUpdate } from './ejecucion-de-escalamiento-update';

describe('EjecucionDeEscalamiento Management Update Component', () => {
  let comp: EjecucionDeEscalamientoUpdate;
  let fixture: ComponentFixture<EjecucionDeEscalamientoUpdate>;
  let activatedRoute: ActivatedRoute;
  let ejecucionDeEscalamientoFormService: EjecucionDeEscalamientoFormService;
  let ejecucionDeEscalamientoService: EjecucionDeEscalamientoService;
  let incidenteService: IncidenteService;
  let pasoEscalamientoService: PasoEscalamientoService;
  let userService: UserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    });

    fixture = TestBed.createComponent(EjecucionDeEscalamientoUpdate);
    activatedRoute = TestBed.inject(ActivatedRoute);
    ejecucionDeEscalamientoFormService = TestBed.inject(EjecucionDeEscalamientoFormService);
    ejecucionDeEscalamientoService = TestBed.inject(EjecucionDeEscalamientoService);
    incidenteService = TestBed.inject(IncidenteService);
    pasoEscalamientoService = TestBed.inject(PasoEscalamientoService);
    userService = TestBed.inject(UserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call Incidente query and add missing value', () => {
      const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = { id: 23926 };
      const incidente: IIncidente = { id: 31968 };
      ejecucionDeEscalamiento.incidente = incidente;

      const incidenteCollection: IIncidente[] = [{ id: 31968 }];
      vitest.spyOn(incidenteService, 'query').mockReturnValue(of(new HttpResponse({ body: incidenteCollection })));
      const additionalIncidentes = [incidente];
      const expectedCollection: IIncidente[] = [...additionalIncidentes, ...incidenteCollection];
      vitest.spyOn(incidenteService, 'addIncidenteToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ ejecucionDeEscalamiento });
      comp.ngOnInit();

      expect(incidenteService.query).toHaveBeenCalled();
      expect(incidenteService.addIncidenteToCollectionIfMissing).toHaveBeenCalledWith(
        incidenteCollection,
        ...additionalIncidentes.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.incidentesSharedCollection()).toEqual(expectedCollection);
    });

    it('should call PasoEscalamiento query and add missing value', () => {
      const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = { id: 23926 };
      const paso: IPasoEscalamiento = { id: 11127 };
      ejecucionDeEscalamiento.paso = paso;

      const pasoEscalamientoCollection: IPasoEscalamiento[] = [{ id: 11127 }];
      vitest.spyOn(pasoEscalamientoService, 'query').mockReturnValue(of(new HttpResponse({ body: pasoEscalamientoCollection })));
      const additionalPasoEscalamientos = [paso];
      const expectedCollection: IPasoEscalamiento[] = [...additionalPasoEscalamientos, ...pasoEscalamientoCollection];
      vitest.spyOn(pasoEscalamientoService, 'addPasoEscalamientoToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ ejecucionDeEscalamiento });
      comp.ngOnInit();

      expect(pasoEscalamientoService.query).toHaveBeenCalled();
      expect(pasoEscalamientoService.addPasoEscalamientoToCollectionIfMissing).toHaveBeenCalledWith(
        pasoEscalamientoCollection,
        ...additionalPasoEscalamientos.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.pasoEscalamientosSharedCollection()).toEqual(expectedCollection);
    });

    it('should call User query and add missing value', () => {
      const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = { id: 23926 };
      const destinatario: IUser = { id: 3944 };
      ejecucionDeEscalamiento.destinatario = destinatario;

      const userCollection: IUser[] = [{ id: 3944 }];
      vitest.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [destinatario];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      vitest.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ ejecucionDeEscalamiento });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(i => expect.objectContaining(i) as typeof i),
      );
      expect(comp.usersSharedCollection()).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = { id: 23926 };
      const incidente: IIncidente = { id: 31968 };
      ejecucionDeEscalamiento.incidente = incidente;
      const paso: IPasoEscalamiento = { id: 11127 };
      ejecucionDeEscalamiento.paso = paso;
      const destinatario: IUser = { id: 3944 };
      ejecucionDeEscalamiento.destinatario = destinatario;

      activatedRoute.data = of({ ejecucionDeEscalamiento });
      comp.ngOnInit();

      expect(comp.incidentesSharedCollection()).toContainEqual(incidente);
      expect(comp.pasoEscalamientosSharedCollection()).toContainEqual(paso);
      expect(comp.usersSharedCollection()).toContainEqual(destinatario);
      expect(comp.ejecucionDeEscalamiento).toEqual(ejecucionDeEscalamiento);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<IEjecucionDeEscalamiento>();
      const ejecucionDeEscalamiento = { id: 24172 };
      vitest.spyOn(ejecucionDeEscalamientoFormService, 'getEjecucionDeEscalamiento').mockReturnValue(ejecucionDeEscalamiento);
      vitest.spyOn(ejecucionDeEscalamientoService, 'update').mockReturnValue(saveSubject);
      vitest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ ejecucionDeEscalamiento });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(ejecucionDeEscalamiento);
      saveSubject.complete();

      // THEN
      expect(ejecucionDeEscalamientoFormService.getEjecucionDeEscalamiento).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(ejecucionDeEscalamientoService.update).toHaveBeenCalledWith(expect.objectContaining(ejecucionDeEscalamiento));
      expect(comp.isSaving()).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<IEjecucionDeEscalamiento>();
      const ejecucionDeEscalamiento = { id: 24172 };
      vitest.spyOn(ejecucionDeEscalamientoFormService, 'getEjecucionDeEscalamiento').mockReturnValue({ id: null });
      vitest.spyOn(ejecucionDeEscalamientoService, 'create').mockReturnValue(saveSubject);
      vitest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ ejecucionDeEscalamiento: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.next(ejecucionDeEscalamiento);
      saveSubject.complete();

      // THEN
      expect(ejecucionDeEscalamientoFormService.getEjecucionDeEscalamiento).toHaveBeenCalled();
      expect(ejecucionDeEscalamientoService.create).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<IEjecucionDeEscalamiento>();
      const ejecucionDeEscalamiento = { id: 24172 };
      vitest.spyOn(ejecucionDeEscalamientoService, 'update').mockReturnValue(saveSubject);
      vitest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ ejecucionDeEscalamiento });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving()).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(ejecucionDeEscalamientoService.update).toHaveBeenCalled();
      expect(comp.isSaving()).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareIncidente', () => {
      it('should forward to incidenteService', () => {
        const entity = { id: 31968 };
        const entity2 = { id: 10195 };
        vitest.spyOn(incidenteService, 'compareIncidente');
        comp.compareIncidente(entity, entity2);
        expect(incidenteService.compareIncidente).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('comparePasoEscalamiento', () => {
      it('should forward to pasoEscalamientoService', () => {
        const entity = { id: 11127 };
        const entity2 = { id: 32102 };
        vitest.spyOn(pasoEscalamientoService, 'comparePasoEscalamiento');
        comp.comparePasoEscalamiento(entity, entity2);
        expect(pasoEscalamientoService.comparePasoEscalamiento).toHaveBeenCalledWith(entity, entity2);
      });
    });

    describe('compareUser', () => {
      it('should forward to userService', () => {
        const entity = { id: 3944 };
        const entity2 = { id: 6275 };
        vitest.spyOn(userService, 'compareUser');
        comp.compareUser(entity, entity2);
        expect(userService.compareUser).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
