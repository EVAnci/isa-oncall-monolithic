import {
  entityConfirmDeleteButtonSelector,
  entityCreateButtonSelector,
  entityCreateCancelButtonSelector,
  entityCreateSaveButtonSelector,
  entityDeleteButtonSelector,
  entityDetailsBackButtonSelector,
  entityDetailsButtonSelector,
  entityEditButtonSelector,
  entityTableSelector,
} from '../../support/entity';

describe('EjecucionDeEscalamiento e2e test', () => {
  const ejecucionDeEscalamientoPageUrl = '/ejecucion-de-escalamiento';
  let username: string;
  let password: string;
  const ejecucionDeEscalamientoSample = { vuelta: 1, nivel: 0, resultado: 'AGOTADO', ejecutadoEn: '2026-10-23T06:06:53.580Z' };

  let ejecucionDeEscalamiento;
  let incidente;

  before(() => {
    cy.credentials().then(credentials => {
      ({ username, password } = credentials);
    });
  });

  beforeEach(() => {
    cy.login(username, password);
  });

  beforeEach(() => {
    // create an instance at the required relationship entity:
    cy.authenticatedRequest({
      method: 'POST',
      url: '/api/incidentes',
      body: {
        titulo: 'yahoo that',
        descripcion: 'who what loosely',
        severidad: 'SEV4',
        estado: 'CERRADO',
        detectadoEn: '2023-12-04T19:11:41.114Z',
        reconocidoEn: '2023-12-04T01:03:17.274Z',
        mitigadoEn: '2023-12-04T15:11:18.120Z',
        resueltoEn: '2023-12-04T01:27:32.932Z',
        usuariosAfectados: 7696,
        cumplioObjetivo: true,
        silenciadoHasta: '2023-12-04T08:18:45.586Z',
      },
    }).then(({ body }) => {
      incidente = body;
    });
  });

  beforeEach(() => {
    cy.intercept('GET', '/api/ejecucion-de-escalamientos+(?*|)').as('entitiesRequest');
    cy.intercept('POST', '/api/ejecucion-de-escalamientos').as('postEntityRequest');
    cy.intercept('DELETE', '/api/ejecucion-de-escalamientos/*').as('deleteEntityRequest');
  });

  beforeEach(() => {
    // Simulate relationships api for better performance and reproducibility.
    cy.intercept('GET', '/api/incidentes', {
      statusCode: 200,
      body: [incidente],
    });

    cy.intercept('GET', '/api/paso-escalamientos', {
      statusCode: 200,
      body: [],
    });

    cy.intercept('GET', '/api/users', {
      statusCode: 200,
      body: [],
    });
  });

  afterEach(() => {
    if (ejecucionDeEscalamiento) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/ejecucion-de-escalamientos/${ejecucionDeEscalamiento.id}`,
      }).then(() => {
        ejecucionDeEscalamiento = undefined;
      });
    }
  });

  afterEach(() => {
    if (incidente) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/incidentes/${incidente.id}`,
      }).then(() => {
        incidente = undefined;
      });
    }
  });

  it('EjecucionDeEscalamientos menu should load EjecucionDeEscalamientos page', () => {
    cy.visit('/');
    cy.clickOnEntityMenuItem('ejecucion-de-escalamiento');
    cy.wait('@entitiesRequest').then(({ response }) => {
      if (response?.body.length === 0) {
        cy.get(entityTableSelector).should('not.exist');
      } else {
        cy.get(entityTableSelector).should('exist');
      }
    });
    cy.getEntityHeading('EjecucionDeEscalamiento').should('exist');
    cy.location('pathname').should('eq', ejecucionDeEscalamientoPageUrl);
  });

  describe('EjecucionDeEscalamiento page', () => {
    it('should have translated page title', () => {
      cy.visit(ejecucionDeEscalamientoPageUrl);
      cy.getEntityHeading('EjecucionDeEscalamiento').should('not.contain', 'oncallApp.ejecucionDeEscalamiento.home.title');
    });

    describe('create button click', () => {
      beforeEach(() => {
        cy.visit(ejecucionDeEscalamientoPageUrl);
        cy.wait('@entitiesRequest');
      });

      it('should load create EjecucionDeEscalamiento page', () => {
        cy.get(entityCreateButtonSelector).click();
        cy.location('pathname').should('eq', `${ejecucionDeEscalamientoPageUrl}/new`);
        cy.getEntityCreateUpdateHeading('EjecucionDeEscalamiento');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', ejecucionDeEscalamientoPageUrl);
      });
    });

    describe('with existing value', () => {
      beforeEach(() => {
        cy.authenticatedRequest({
          method: 'POST',
          url: '/api/ejecucion-de-escalamientos',
          body: {
            ...ejecucionDeEscalamientoSample,
            incidente,
          },
        }).then(({ body }) => {
          ejecucionDeEscalamiento = body;

          cy.intercept(
            {
              method: 'GET',
              url: '/api/ejecucion-de-escalamientos+(?*|)',
              times: 1,
            },
            {
              statusCode: 200,
              headers: {
                link: '<http://localhost/api/ejecucion-de-escalamientos?page=0&size=20>; rel="last",<http://localhost/api/ejecucion-de-escalamientos?page=0&size=20>; rel="first"',
              },
              body: [ejecucionDeEscalamiento],
            },
          ).as('entitiesRequestInternal');
        });

        cy.visit(ejecucionDeEscalamientoPageUrl);

        cy.wait('@entitiesRequestInternal');
      });

      it('detail button click should load details EjecucionDeEscalamiento page', () => {
        cy.get(entityDetailsButtonSelector).first().click();
        cy.getEntityDetailsHeading('ejecucionDeEscalamiento');
        cy.get(entityDetailsBackButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', ejecucionDeEscalamientoPageUrl);
      });

      it('edit button click should load edit EjecucionDeEscalamiento page and go back', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('EjecucionDeEscalamiento');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', ejecucionDeEscalamientoPageUrl);
      });

      it('edit button click should load edit EjecucionDeEscalamiento page and save', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('EjecucionDeEscalamiento');
        cy.get(entityCreateSaveButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', ejecucionDeEscalamientoPageUrl);
      });

      it('last delete button click should delete instance of EjecucionDeEscalamiento', () => {
        cy.get(entityDeleteButtonSelector).last().click();
        cy.getEntityDeleteDialogHeading('ejecucionDeEscalamiento').should('exist');
        cy.get(entityConfirmDeleteButtonSelector).click();
        cy.wait('@deleteEntityRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(204);
        });
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', ejecucionDeEscalamientoPageUrl);

        ejecucionDeEscalamiento = undefined;
      });
    });
  });

  describe('new EjecucionDeEscalamiento page', () => {
    beforeEach(() => {
      cy.visit(ejecucionDeEscalamientoPageUrl);
      cy.get(entityCreateButtonSelector).click();
      cy.getEntityCreateUpdateHeading('EjecucionDeEscalamiento');
    });

    it('should create an instance of EjecucionDeEscalamiento', () => {
      cy.get(`[data-cy="vuelta"]`).type('5');
      cy.get(`[data-cy="vuelta"]`).should('have.value', '5');

      cy.get(`[data-cy="nivel"]`).type('7');
      cy.get(`[data-cy="nivel"]`).should('have.value', '7');

      cy.get(`[data-cy="resultado"]`).select('SIN_POLITICA');

      cy.get(`[data-cy="ejecutadoEn"]`).type('2026-10-22T23:13');
      cy.get(`[data-cy="ejecutadoEn"]`).blur();
      cy.get(`[data-cy="ejecutadoEn"]`).should('have.value', '2026-10-22T23:13');

      cy.get(`[data-cy="proximoEn"]`).type('2026-10-23T09:34');
      cy.get(`[data-cy="proximoEn"]`).blur();
      cy.get(`[data-cy="proximoEn"]`).should('have.value', '2026-10-23T09:34');

      cy.get(`[data-cy="detalle"]`).type('incidentally');
      cy.get(`[data-cy="detalle"]`).should('have.value', 'incidentally');

      cy.get(`[data-cy="incidente"]`).select(1);

      cy.get(entityCreateSaveButtonSelector).click();

      cy.wait('@postEntityRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(201);
        ejecucionDeEscalamiento = response.body;
      });
      cy.wait('@entitiesRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
      });
      cy.location('pathname').should('eq', ejecucionDeEscalamientoPageUrl);
    });
  });
});
