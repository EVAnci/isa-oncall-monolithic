import { HttpHeaders } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, WritableSignal, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Data, ParamMap, Router, RouterLink } from '@angular/router';

import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap/modal';
import { TranslatePipe } from '@ngx-translate/core';
import { InfiniteScrollDirective } from 'ngx-infinite-scroll';
import { Subscription, combineLatest, filter, tap } from 'rxjs';

import { DEFAULT_SORT_DATA, ITEM_DELETED_EVENT, SORT } from 'app/config/navigation.constants';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { ParseLinks } from 'app/core/util/parse-links.service';
import { Alert } from 'app/shared/alert/alert';
import { AlertError } from 'app/shared/alert/alert-error';
import { FormatMediumDatetimePipe } from 'app/shared/date';
import { TranslateDirective } from 'app/shared/language';
import { SortByDirective, SortDirective, SortService, type SortState, sortStateSignal } from 'app/shared/sort';
import { EjecucionDeEscalamientoDeleteDialog } from '../delete/ejecucion-de-escalamiento-delete-dialog';
import { IEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';
import { EjecucionDeEscalamientoService } from '../service/ejecucion-de-escalamiento.service';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'jhi-ejecucion-de-escalamiento',
  templateUrl: './ejecucion-de-escalamiento.html',
  imports: [
    RouterLink,
    FormsModule,
    FontAwesomeModule,
    AlertError,
    Alert,
    SortDirective,
    SortByDirective,
    TranslateDirective,
    TranslatePipe,
    FormatMediumDatetimePipe,
    InfiniteScrollDirective,
  ],
})
export class EjecucionDeEscalamiento implements OnInit {
  subscription: Subscription | null = null;
  readonly ejecucionDeEscalamientos = signal<IEjecucionDeEscalamiento[]>([]);

  sortState = sortStateSignal({});

  readonly itemsPerPage = signal(ITEMS_PER_PAGE);
  readonly links: WritableSignal<Record<string, undefined | Record<string, string | undefined>>> = signal({});
  readonly hasMorePage = computed(() => !!this.links().next);
  readonly isFirstFetch = computed(() => Object.keys(this.links()).length === 0);

  readonly router = inject(Router);
  protected readonly ejecucionDeEscalamientoService = inject(EjecucionDeEscalamientoService);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  readonly isLoading = this.ejecucionDeEscalamientoService.ejecucionDeEscalamientosResource.isLoading;
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly sortService = inject(SortService);
  protected parseLinks = inject(ParseLinks);
  protected modalService = inject(NgbModal);

  constructor() {
    effect(() => {
      const headers = this.ejecucionDeEscalamientoService.ejecucionDeEscalamientosResource.headers();
      if (headers) {
        this.fillComponentAttributesFromResponseHeader(headers);
      }
    });
    effect(() => {
      this.ejecucionDeEscalamientos.update(ejecucionDeEscalamientos =>
        this.fillComponentAttributesFromResponseBody(
          [...this.ejecucionDeEscalamientoService.ejecucionDeEscalamientos()],
          ejecucionDeEscalamientos,
        ),
      );
    });
  }

  trackId = (item: IEjecucionDeEscalamiento): number => this.ejecucionDeEscalamientoService.getEjecucionDeEscalamientoIdentifier(item);

  ngOnInit(): void {
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => this.reset()),
        tap(() => this.load()),
      )
      .subscribe();
  }

  reset(): void {
    this.ejecucionDeEscalamientos.set([]);
  }

  loadNextPage(): void {
    this.load();
  }

  delete(ejecucionDeEscalamiento: IEjecucionDeEscalamiento): void {
    const modalRef = this.modalService.open(EjecucionDeEscalamientoDeleteDialog, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.ejecucionDeEscalamiento = ejecucionDeEscalamiento;
    // unsubscribe not needed because closed completes on modal close
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  load(): void {
    this.queryBackend();
  }

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(event);
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected fillComponentAttributesFromResponseBody(
    data: IEjecucionDeEscalamiento[],
    currentValue: IEjecucionDeEscalamiento[],
  ): IEjecucionDeEscalamiento[] {
    const ejecucionDeEscalamientosNew = [...currentValue];
    for (const d of data) {
      if (!ejecucionDeEscalamientosNew.some(op => op.id === d.id)) {
        ejecucionDeEscalamientosNew.push(d);
      }
    }
    return ejecucionDeEscalamientosNew;
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    const linkHeader = headers.get('link');
    if (linkHeader) {
      this.links.set(this.parseLinks.parseAll(linkHeader));
    } else {
      this.links.set({});
    }
  }

  protected queryBackend(): void {
    const queryObject: any = {
      size: this.itemsPerPage(),
      eagerload: true,
    };
    if (this.hasMorePage()) {
      Object.assign(queryObject, this.links().next);
    } else if (this.isFirstFetch()) {
      Object.assign(queryObject, { sort: this.sortService.buildSortParam(this.sortState()) });
    }

    this.ejecucionDeEscalamientoService.ejecucionDeEscalamientosParams.set(queryObject);
  }

  protected handleNavigation(sortState: SortState): void {
    this.links.set({});

    const queryParamsObj = {
      sort: this.sortService.buildSortParam(sortState),
    };

    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsObj,
    });
  }
}
