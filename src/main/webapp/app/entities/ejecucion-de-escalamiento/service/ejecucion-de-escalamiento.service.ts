import { HttpClient, HttpResponse, httpResource } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import dayjs from 'dayjs/esm';
import { Observable, map } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { isPresent } from 'app/core/util/operators';
import { IEjecucionDeEscalamiento, NewEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';

export type PartialUpdateEjecucionDeEscalamiento = Partial<IEjecucionDeEscalamiento> & Pick<IEjecucionDeEscalamiento, 'id'>;

type RestOf<T extends IEjecucionDeEscalamiento | NewEjecucionDeEscalamiento> = Omit<T, 'ejecutadoEn' | 'proximoEn'> & {
  ejecutadoEn?: string | null;
  proximoEn?: string | null;
};

export type RestEjecucionDeEscalamiento = RestOf<IEjecucionDeEscalamiento>;

export type NewRestEjecucionDeEscalamiento = RestOf<NewEjecucionDeEscalamiento>;

export type PartialUpdateRestEjecucionDeEscalamiento = RestOf<PartialUpdateEjecucionDeEscalamiento>;

@Injectable()
export class EjecucionDeEscalamientosService {
  readonly ejecucionDeEscalamientosParams = signal<
    Record<string, string | number | boolean | readonly (string | number | boolean)[]> | undefined
  >(undefined);
  readonly ejecucionDeEscalamientosResource = httpResource<RestEjecucionDeEscalamiento[]>(() => {
    const params = this.ejecucionDeEscalamientosParams();
    if (!params) {
      return undefined;
    }
    return { url: this.resourceUrl, params };
  });
  /**
   * This signal holds the list of ejecucionDeEscalamiento that have been fetched. It is updated when the ejecucionDeEscalamientosResource emits a new value.
   * In case of error while fetching the ejecucionDeEscalamientos, the signal is set to an empty array.
   */
  readonly ejecucionDeEscalamientos = computed(() =>
    (this.ejecucionDeEscalamientosResource.hasValue() ? this.ejecucionDeEscalamientosResource.value() : []).map(item =>
      this.convertValueFromServer(item),
    ),
  );
  protected readonly applicationConfigService = inject(ApplicationConfigService);
  protected readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/ejecucion-de-escalamientos');

  protected convertValueFromServer(restEjecucionDeEscalamiento: RestEjecucionDeEscalamiento): IEjecucionDeEscalamiento {
    return {
      ...restEjecucionDeEscalamiento,
      ejecutadoEn: restEjecucionDeEscalamiento.ejecutadoEn ? dayjs(restEjecucionDeEscalamiento.ejecutadoEn) : undefined,
      proximoEn: restEjecucionDeEscalamiento.proximoEn ? dayjs(restEjecucionDeEscalamiento.proximoEn) : undefined,
    };
  }
}

@Injectable({ providedIn: 'root' })
export class EjecucionDeEscalamientoService extends EjecucionDeEscalamientosService {
  protected readonly http = inject(HttpClient);

  create(ejecucionDeEscalamiento: NewEjecucionDeEscalamiento): Observable<IEjecucionDeEscalamiento> {
    const copy = this.convertValueFromClient(ejecucionDeEscalamiento);
    return this.http.post<RestEjecucionDeEscalamiento>(this.resourceUrl, copy).pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(ejecucionDeEscalamiento: IEjecucionDeEscalamiento): Observable<IEjecucionDeEscalamiento> {
    const copy = this.convertValueFromClient(ejecucionDeEscalamiento);
    return this.http
      .put<RestEjecucionDeEscalamiento>(
        `${this.resourceUrl}/${encodeURIComponent(this.getEjecucionDeEscalamientoIdentifier(ejecucionDeEscalamiento))}`,
        copy,
      )
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(ejecucionDeEscalamiento: PartialUpdateEjecucionDeEscalamiento): Observable<IEjecucionDeEscalamiento> {
    const copy = this.convertValueFromClient(ejecucionDeEscalamiento);
    return this.http
      .patch<RestEjecucionDeEscalamiento>(
        `${this.resourceUrl}/${encodeURIComponent(this.getEjecucionDeEscalamientoIdentifier(ejecucionDeEscalamiento))}`,
        copy,
      )
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<IEjecucionDeEscalamiento> {
    return this.http
      .get<RestEjecucionDeEscalamiento>(`${this.resourceUrl}/${encodeURIComponent(id)}`)
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<HttpResponse<IEjecucionDeEscalamiento[]>> {
    const options = createRequestOption(req);
    return this.http
      .get<RestEjecucionDeEscalamiento[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => res.clone({ body: this.convertResponseArrayFromServer(res.body!) })));
  }

  delete(id: number): Observable<undefined> {
    return this.http.delete<undefined>(`${this.resourceUrl}/${encodeURIComponent(id)}`);
  }

  getEjecucionDeEscalamientoIdentifier(ejecucionDeEscalamiento: Pick<IEjecucionDeEscalamiento, 'id'>): number {
    return ejecucionDeEscalamiento.id;
  }

  compareEjecucionDeEscalamiento(
    o1: Pick<IEjecucionDeEscalamiento, 'id'> | null,
    o2: Pick<IEjecucionDeEscalamiento, 'id'> | null,
  ): boolean {
    return o1 && o2 ? this.getEjecucionDeEscalamientoIdentifier(o1) === this.getEjecucionDeEscalamientoIdentifier(o2) : o1 === o2;
  }

  addEjecucionDeEscalamientoToCollectionIfMissing<Type extends Pick<IEjecucionDeEscalamiento, 'id'>>(
    ejecucionDeEscalamientoCollection: Type[],
    ...ejecucionDeEscalamientosToCheck: (Type | null | undefined)[]
  ): Type[] {
    const ejecucionDeEscalamientos: Type[] = ejecucionDeEscalamientosToCheck.filter(isPresent);
    if (ejecucionDeEscalamientos.length > 0) {
      const ejecucionDeEscalamientoCollectionIdentifiers = ejecucionDeEscalamientoCollection.map(ejecucionDeEscalamientoItem =>
        this.getEjecucionDeEscalamientoIdentifier(ejecucionDeEscalamientoItem),
      );
      const ejecucionDeEscalamientosToAdd = ejecucionDeEscalamientos.filter(ejecucionDeEscalamientoItem => {
        const ejecucionDeEscalamientoIdentifier = this.getEjecucionDeEscalamientoIdentifier(ejecucionDeEscalamientoItem);
        if (ejecucionDeEscalamientoCollectionIdentifiers.includes(ejecucionDeEscalamientoIdentifier)) {
          return false;
        }
        ejecucionDeEscalamientoCollectionIdentifiers.push(ejecucionDeEscalamientoIdentifier);
        return true;
      });
      return [...ejecucionDeEscalamientosToAdd, ...ejecucionDeEscalamientoCollection];
    }
    return ejecucionDeEscalamientoCollection;
  }

  protected convertValueFromClient<T extends IEjecucionDeEscalamiento | NewEjecucionDeEscalamiento | PartialUpdateEjecucionDeEscalamiento>(
    ejecucionDeEscalamiento: T,
  ): RestOf<T> {
    return {
      ...ejecucionDeEscalamiento,
      ejecutadoEn: ejecucionDeEscalamiento.ejecutadoEn?.toJSON() ?? null,
      proximoEn: ejecucionDeEscalamiento.proximoEn?.toJSON() ?? null,
    };
  }

  protected convertResponseFromServer(res: RestEjecucionDeEscalamiento): IEjecucionDeEscalamiento {
    return this.convertValueFromServer(res);
  }

  protected convertResponseArrayFromServer(res: RestEjecucionDeEscalamiento[]): IEjecucionDeEscalamiento[] {
    return res.map(item => this.convertValueFromServer(item));
  }
}
