const assert = require('assert');

describe('webdriver.io page', () => {
    it('should have the right title', async function () {        
        await browser.url('https://webdriver.io');
        await browser.pause(1000);
        const title = await browser.getTitle();        
        assert.equal(title, 'WebdriverIO · Next-gen WebDriver test framework for Node.js');        
    })    
});