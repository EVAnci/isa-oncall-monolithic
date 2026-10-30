import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap/modal';

import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { AlertError } from 'app/shared/alert/alert-error';
import { TranslateDirective } from 'app/shared/language';
import { IEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';
import { EjecucionDeEscalamientoService } from '../service/ejecucion-de-escalamiento.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './ejecucion-de-escalamiento-delete-dialog.html',
  imports: [TranslateDirective, FormsModule, FontAwesomeModule, AlertError],
})
export class EjecucionDeEscalamientoDeleteDialog {
  ejecucionDeEscalamiento?: IEjecucionDeEscalamiento;

  protected readonly ejecucionDeEscalamientoService = inject(EjecucionDeEscalamientoService);
  protected readonly activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.ejecucionDeEscalamientoService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
