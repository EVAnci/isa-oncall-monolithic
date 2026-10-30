import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IEjecucionDeEscalamiento } from '../ejecucion-de-escalamiento.model';
import {
  sampleWithFullData,
  sampleWithNewData,
  sampleWithPartialData,
  sampleWithRequiredData,
} from '../ejecucion-de-escalamiento.test-samples';

import { EjecucionDeEscalamientoService, RestEjecucionDeEscalamiento } from './ejecucion-de-escalamiento.service';

const requireRestSample: RestEjecucionDeEscalamiento = {
  ...sampleWithRequiredData,
  ejecutadoEn: sampleWithRequiredData.ejecutadoEn?.toJSON(),
  proximoEn: sampleWithRequiredData.proximoEn?.toJSON(),
};

describe('EjecucionDeEscalamiento Service', () => {
  let service: EjecucionDeEscalamientoService;
  let httpMock: HttpTestingController;
  let expectedResult: IEjecucionDeEscalamiento | IEjecucionDeEscalamiento[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    expectedResult = null;
    service = TestBed.inject(EjecucionDeEscalamientoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a EjecucionDeEscalamiento', () => {
      const ejecucionDeEscalamiento = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(ejecucionDeEscalamiento).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a EjecucionDeEscalamiento', () => {
      const ejecucionDeEscalamiento = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(ejecucionDeEscalamiento).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a EjecucionDeEscalamiento', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of EjecucionDeEscalamiento', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a EjecucionDeEscalamiento', () => {
      service.delete(123).subscribe();

      const requests = httpMock.match({ method: 'DELETE' });
      expect(requests).toHaveLength(1);
    });

    describe('addEjecucionDeEscalamientoToCollectionIfMissing', () => {
      it('should add a EjecucionDeEscalamiento to an empty array', () => {
        const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = sampleWithRequiredData;
        expectedResult = service.addEjecucionDeEscalamientoToCollectionIfMissing([], ejecucionDeEscalamiento);
        expect(expectedResult).toEqual([ejecucionDeEscalamiento]);
      });

      it('should not add a EjecucionDeEscalamiento to an array that contains it', () => {
        const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = sampleWithRequiredData;
        const ejecucionDeEscalamientoCollection: IEjecucionDeEscalamiento[] = [
          {
            ...ejecucionDeEscalamiento,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addEjecucionDeEscalamientoToCollectionIfMissing(
          ejecucionDeEscalamientoCollection,
          ejecucionDeEscalamiento,
        );
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a EjecucionDeEscalamiento to an array that doesn't contain it", () => {
        const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = sampleWithRequiredData;
        const ejecucionDeEscalamientoCollection: IEjecucionDeEscalamiento[] = [sampleWithPartialData];
        expectedResult = service.addEjecucionDeEscalamientoToCollectionIfMissing(
          ejecucionDeEscalamientoCollection,
          ejecucionDeEscalamiento,
        );
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(ejecucionDeEscalamiento);
      });

      it('should add only unique EjecucionDeEscalamiento to an array', () => {
        const ejecucionDeEscalamientoArray: IEjecucionDeEscalamiento[] = [
          sampleWithRequiredData,
          sampleWithPartialData,
          sampleWithFullData,
        ];
        const ejecucionDeEscalamientoCollection: IEjecucionDeEscalamiento[] = [sampleWithRequiredData];
        expectedResult = service.addEjecucionDeEscalamientoToCollectionIfMissing(
          ejecucionDeEscalamientoCollection,
          ...ejecucionDeEscalamientoArray,
        );
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = sampleWithRequiredData;
        const ejecucionDeEscalamiento2: IEjecucionDeEscalamiento = sampleWithPartialData;
        expectedResult = service.addEjecucionDeEscalamientoToCollectionIfMissing([], ejecucionDeEscalamiento, ejecucionDeEscalamiento2);
        expect(expectedResult).toEqual([ejecucionDeEscalamiento, ejecucionDeEscalamiento2]);
      });

      it('should accept null and undefined values', () => {
        const ejecucionDeEscalamiento: IEjecucionDeEscalamiento = sampleWithRequiredData;
        expectedResult = service.addEjecucionDeEscalamientoToCollectionIfMissing([], null, ejecucionDeEscalamiento, undefined);
        expect(expectedResult).toEqual([ejecucionDeEscalamiento]);
      });

      it('should return initial array if no EjecucionDeEscalamiento is added', () => {
        const ejecucionDeEscalamientoCollection: IEjecucionDeEscalamiento[] = [sampleWithRequiredData];
        expectedResult = service.addEjecucionDeEscalamientoToCollectionIfMissing(ejecucionDeEscalamientoCollection, undefined, null);
        expect(expectedResult).toEqual(ejecucionDeEscalamientoCollection);
      });
    });

    describe('compareEjecucionDeEscalamiento', () => {
      it('should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareEjecucionDeEscalamiento(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('should return false if one entity is null', () => {
        const entity1 = { id: 24172 };
        const entity2 = null;

        const compareResult1 = service.compareEjecucionDeEscalamiento(entity1, entity2);
        const compareResult2 = service.compareEjecucionDeEscalamiento(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey differs', () => {
        const entity1 = { id: 24172 };
        const entity2 = { id: 23926 };

        const compareResult1 = service.compareEjecucionDeEscalamiento(entity1, entity2);
        const compareResult2 = service.compareEjecucionDeEscalamiento(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('should return false if primaryKey matches', () => {
        const entity1 = { id: 24172 };
        const entity2 = { id: 24172 };

        const compareResult1 = service.compareEjecucionDeEscalamiento(entity1, entity2);
        const compareResult2 = service.compareEjecucionDeEscalamiento(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
