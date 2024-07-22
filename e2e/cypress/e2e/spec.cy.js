describe('End-to-end test', () => {
    beforeEach(() => {
        cy.visit('/')
    })
    it('logs into SAN', () => {
        cy.get('button').contains('Create handover link').click()
        cy.get('a.govuk-button').contains('Open').invoke('attr', 'href').then((href) => {
            cy.visit(href)
        })
        cy.get('.hmpps-header__title__service-name').should('contain.text', 'Strengths and needs')
    })
})
