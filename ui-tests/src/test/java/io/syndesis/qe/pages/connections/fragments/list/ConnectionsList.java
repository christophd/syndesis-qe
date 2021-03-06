package io.syndesis.qe.pages.connections.fragments.list;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import io.syndesis.qe.fragments.common.list.CardList;
import io.syndesis.qe.fragments.common.list.actions.ListAction;
import io.syndesis.qe.fragments.common.menu.KebabMenu;
import io.syndesis.qe.pages.ModalDialogPage;
import io.syndesis.qe.utils.ByUtils;
import io.syndesis.qe.utils.TestUtils;

import org.openqa.selenium.By;

import com.codeborne.selenide.SelenideElement;

public class ConnectionsList extends CardList {

    private static final class Element {
        public static final By TECH_PREVIEW = By.xpath("syndesis-card-tech-preview");
        public static final String CONNECTION_CARD = "*[data-testid=\"connection-card-%s-card\"]";
        public static final By CONNECTION_CARD_TITLE = ByUtils.dataTestId("connection-card-title");
        public static String KEBAB_MENU_SELECTOR = "[id=\"connection-%s-menu\"]";
    }

    public ConnectionsList(By rootElement) {
        super(rootElement);
    }

    @Override
    public void invokeActionOnItem(String title, ListAction action) {

        switch (action) {
            case DELETE:
                KebabMenu kebabMenu = new KebabMenu($(kebabMenu(title)).shouldBe(visible));
                kebabMenu.open();
                kebabMenu.getItemElement("Delete").shouldBe(visible).click();
                TestUtils.sleepForJenkinsDelayIfHigher(3);
                new ModalDialogPage().getButton("Delete").shouldBe(visible).click();
                break;
            case CLICK:
                getItem(title)
                    .find(Element.CONNECTION_CARD_TITLE)
                    .shouldBe(visible)
                    .click();
                break;
            default:
                super.invokeActionOnItem(title, action);
        }
    }

    @Override
    public SelenideElement getItem(String title) {
        return $(By.cssSelector(String.format(Element.CONNECTION_CARD,
            title.toLowerCase()
                .replaceAll("\\s|\\)", "-")
                .replaceAll("\\(", ""))));
    }

    public boolean isConnectionTechPreview(String title) {
        SelenideElement item = getItem(title).parent().$(Element.TECH_PREVIEW);
        return "Technology Preview".equals(item.getText());
    }

    private By kebabMenu(String title) {
        return By.cssSelector(String.format(Element.KEBAB_MENU_SELECTOR, title));
    }
}
