import org.junit.Assert;
import org.junit.Test;

public class LocalizeActionTest {
    @Test
    public void testGetTranslationId() {
        final String result = Util.getTranslationId(".foo-bar. apple.");
        final String expected = "FOO_BAR_APPLE";

        Assert.assertEquals(result, expected);
    }
}