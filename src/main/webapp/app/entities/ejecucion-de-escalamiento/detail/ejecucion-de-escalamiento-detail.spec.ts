import { beforeEach, describe, expect, it, vitest } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { faArrowLeft, faPencilAlt } from '@fortawesome/free-solid-svg-icons';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { EjecucionDeEscalamientoDetail } from './ejecucion-de-escalamiento-detail';

describe('EjecucionDeEscalamiento Management Detail Component', () => {
  let comp: EjecucionDeEscalamientoDetail;
  let fixture: ComponentFixture<EjecucionDeEscalamientoDetail>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        provideRouter(
          [
            {
              path: '**',
              loadComponent: () => import('./ejecucion-de-escalamiento-detail').then(m => m.EjecucionDeEscalamientoDetail),
              resolve: { ejecucionDeEscalamiento: () => of({ id: 24172 }) },
            },
          ],
          withComponentInputBinding(),
        ),
      ],
    });
    const library = TestBed.inject(FaIconLibrary);
    library.addIcons(faArrowLeft);
    library.addIcons(faPencilAlt);
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EjecucionDeEscalamientoDetail);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('should load ejecucionDeEscalamiento on init', async () => {
      const harness = await RouterTestingHarness.create();
      const instance = await harness.navigateByUrl('/', EjecucionDeEscalamientoDetail);

      // THEN
      expect(instance.ejecucionDeEscalamiento()).toEqual(expect.objectContaining({ id: 24172 }));
    });
  });

  describe('PreviousState', () => {
    it('should navigate to previous state', () => {
      vitest.spyOn(globalThis.history, 'back');
      comp.previousState();
      expect(globalThis.history.back).toHaveBeenCalled();
    });
  });
});
