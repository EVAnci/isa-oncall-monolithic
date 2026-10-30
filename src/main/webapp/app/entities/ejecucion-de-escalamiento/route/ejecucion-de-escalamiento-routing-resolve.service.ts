import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { EMPTY, Observable, catchError, of } from 'rxjs';

import { IEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';
import { EjecucionDeEscalamientoService } from '../service/ejecucion-de-escalamiento.service';

const ejecucionDeEscalamientoResolve = (route: ActivatedRouteSnapshot): Observable<null | IEjecucionDeEscalamiento> => {
  const { id } = route.params;
  if (id) {
    const router = inject(Router);
    const service = inject(EjecucionDeEscalamientoService);
    return service.find(id).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 404) {
          router.navigate(['404']);
        } else {
          router.navigate(['error']);
        }
        return EMPTY;
      }),
    );
  }

  return of(null);
};

export default ejecucionDeEscalamientoResolve;
