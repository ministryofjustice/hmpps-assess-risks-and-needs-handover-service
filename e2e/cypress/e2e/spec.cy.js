describe('Create session and generate sentence plan link', () => {
  it('captures the CRN, starts a session, and verifies the sentence-plan page', () => {
    cy.visit('/training-session-launcher/browse?scenario=default');

    let crn;

    // 1. Find the <dt> containing "CRN" and get the sibling <dd> content
    cy.contains('dt', 'CRN')
      .next('dd')
      .invoke('text')
      .then((text) => {
        crn = text.trim();
        expect(crn).to.not.be.empty;
      });

    // 2. Click "Start session" and wait for the alert heading
    cy.contains('button', 'Start session').click();

    cy.get('h2.moj-alert__heading')
      .should('be.visible')
      .and(($h2) => {
        expect($h2.text().replace(/\s+/g, ' ').trim()).to.equal('Session created');
      });

    // 3. Click "Generate link" and verify the URL and CRN on the new page
    cy.contains('button', 'Generate link').click();

    cy.location('pathname').should('satisfy', (pathname) => {
      const firstSegment = pathname.replace(/^\/+/, '').split('/')[0];
      return firstSegment === 'sentence-plan';
    });

    cy.get('dd').then(($dds) => {
      const matchingDd = [...$dds].find(
        (el) => el.textContent.trim() === crn
      );
      expect(matchingDd, `expected a <dd> element with CRN "${crn}"`).to.exist;
    });
  });
});
