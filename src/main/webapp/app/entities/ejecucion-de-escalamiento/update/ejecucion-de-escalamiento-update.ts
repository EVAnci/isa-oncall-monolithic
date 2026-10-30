import { HttpResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable, finalize, map } from 'rxjs';

import { ResultadoEscalamiento } from 'app/entities/enumerations/resultado-escalamiento.model';
import { IIncidente } from 'app/entities/incidente/incidente.model';
import { IncidenteService } from 'app/entities/incidente/service/incidente.service';
import { IPasoEscalamiento } from 'app/entities/paso-escalamiento/paso-escalamiento.model';
import { PasoEscalamientoService } from 'app/entities/paso-escalamiento/service/paso-escalamiento.service';
import { IUser } from 'app/entities/user/user.model';
import { AlertError } from 'app/shared/alert/alert-error';
import { TranslateDirective } from 'app/shared/language';

import { IEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';
import { EjecucionDeEscalamientoService } from '../service/ejecucion-de-escalamiento.service';

import { EjecucionDeEscalamientoFormGroup, EjecucionDeEscalamientoFormService } from './ejecucion-de-escalamiento-form.service';
import { UserService } from 'app/entities/user/service/user.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'jhi-ejecucion-de-escalamiento-update',
  templateUrl: './ejecucion-de-escalamiento-update.html',
  imports: [TranslateDirective, TranslatePipe, FontAwesomeModule, AlertError, ReactiveFormsModule],
})
export class EjecucionDeEscalamientoUpdate implements OnInit {
  readonly isSaving = signal(false);
  ejecucionDeEscalamiento: IEjecucionDeEscalamiento | null = null;
  resultadoEscalamientoValues = Object.keys(ResultadoEscalamiento);

  incidentesSharedCollection = signal<IIncidente[]>([]);
  pasoEscalamientosSharedCollection = signal<IPasoEscalamiento[]>([]);
  usersSharedCollection = signal<IUser[]>([]);

  protected ejecucionDeEscalamientoService = inject(EjecucionDeEscalamientoService);
  protected ejecucionDeEscalamientoFormService = inject(EjecucionDeEscalamientoFormService);
  protected incidenteService = inject(IncidenteService);
  protected pasoEscalamientoService = inject(PasoEscalamientoService);
  protected userService = inject(UserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: EjecucionDeEscalamientoFormGroup = this.ejecucionDeEscalamientoFormService.createEjecucionDeEscalamientoFormGroup();

  compareIncidente = (o1: IIncidente | null, o2: IIncidente | null): boolean => this.incidenteService.compareIncidente(o1, o2);

  comparePasoEscalamiento = (o1: IPasoEscalamiento | null, o2: IPasoEscalamiento | null): boolean =>
    this.pasoEscalamientoService.comparePasoEscalamiento(o1, o2);

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ ejecucionDeEscalamiento }) => {
      this.ejecucionDeEscalamiento = ejecucionDeEscalamiento;
      if (ejecucionDeEscalamiento) {
        this.updateForm(ejecucionDeEscalamiento);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    globalThis.history.back();
  }

  save(): void {
    this.isSaving.set(true);
    const ejecucionDeEscalamiento = this.ejecucionDeEscalamientoFormService.getEjecucionDeEscalamiento(this.editForm);
    if (ejecucionDeEscalamiento.id === null) {
      this.subscribeToSaveResponse(this.ejecucionDeEscalamientoService.create(ejecucionDeEscalamiento));
    } else {
      this.subscribeToSaveResponse(this.ejecucionDeEscalamientoService.update(ejecucionDeEscalamiento));
    }
  }

  protected subscribeToSaveResponse(result: Observable<IEjecucionDeEscalamiento | null>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving.set(false);
  }

  protected updateForm(ejecucionDeEscalamiento: IEjecucionDeEscalamiento): void {
    this.ejecucionDeEscalamiento = ejecucionDeEscalamiento;
    this.ejecucionDeEscalamientoFormService.resetForm(this.editForm, ejecucionDeEscalamiento);

    this.incidentesSharedCollection.update(incidentes =>
      this.incidenteService.addIncidenteToCollectionIfMissing<IIncidente>(incidentes, ejecucionDeEscalamiento.incidente),
    );
    this.pasoEscalamientosSharedCollection.update(pasoEscalamientos =>
      this.pasoEscalamientoService.addPasoEscalamientoToCollectionIfMissing<IPasoEscalamiento>(
        pasoEscalamientos,
        ejecucionDeEscalamiento.paso,
      ),
    );
    this.usersSharedCollection.update(users =>
      this.userService.addUserToCollectionIfMissing<IUser>(users, ejecucionDeEscalamiento.destinatario),
    );
  }

  protected loadRelationshipsOptions(): void {
    this.incidenteService
      .query()
      .pipe(map((res: HttpResponse<IIncidente[]>) => res.body ?? []))
      .pipe(
        map((incidentes: IIncidente[]) =>
          this.incidenteService.addIncidenteToCollectionIfMissing<IIncidente>(incidentes, this.ejecucionDeEscalamiento?.incidente),
        ),
      )
      .subscribe((incidentes: IIncidente[]) => this.incidentesSharedCollection.set(incidentes));

    this.pasoEscalamientoService
      .query()
      .pipe(map((res: HttpResponse<IPasoEscalamiento[]>) => res.body ?? []))
      .pipe(
        map((pasoEscalamientos: IPasoEscalamiento[]) =>
          this.pasoEscalamientoService.addPasoEscalamientoToCollectionIfMissing<IPasoEscalamiento>(
            pasoEscalamientos,
            this.ejecucionDeEscalamiento?.paso,
          ),
        ),
      )
      .subscribe((pasoEscalamientos: IPasoEscalamiento[]) => this.pasoEscalamientosSharedCollection.set(pasoEscalamientos));

    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(
        map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.ejecucionDeEscalamiento?.destinatario)),
      )
      .subscribe((users: IUser[]) => this.usersSharedCollection.set(users));
  }
}
