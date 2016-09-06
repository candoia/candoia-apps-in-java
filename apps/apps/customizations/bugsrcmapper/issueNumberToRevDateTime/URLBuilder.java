package customizations.bugsrcmapper.issueNumberToRevDateTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;


public final class URLBuilder {

    /**
     * Root sources of the API
     *
     * @author ghlp
     */
    enum GithubAPI {
        LEGACY_V2 {
            @Override
            public String baseForm() {
                return String.format("%slegacy/repos/search/", ROOT.baseForm());
            }
        },
        ROOT {
            @Override
            public String baseForm() {
                return "https://api.github.com/";
            }
        },
        REPOSITORIES {
            @Override
            public String baseForm() {
                return String.format("%srepositories", ROOT.baseForm());
            }
        },
        USERS {
            @Override
            public String baseForm() {
                return "https://api.github.com/users/";
            }
        };

        public abstract String baseForm();
    }

    enum SVNAPI {
        ROOT {
            @Override
            public String baseForm() {
                return "http://sourceforge.net/rest/p";
            }
        },

        USER {
            @Override
            public String baseForm() {
                return "http://sourceforge.net/rest/u";
            }
        };

        public abstract String baseForm();
    }

    private String oauthToken;
    private final StringBuilder builder;

    @Inject
    public URLBuilder(@Named("githubOauthToken") String oauthToken) {
        this.oauthToken = oauthToken;
        this.builder = new StringBuilder();
    }

    /**
     * Choose what will be your base API.
     * Currently available: USERS, REPOSITORES, and LEGACY
     * see {@link GithubAPI}

     * @param api
     * @return current url representation
     */
    public URLBuilder uses(GithubAPI api) {
        this.builder.append(api.baseForm());
        return this;
    }

    public URLBuilder uses(SVNAPI api) {
        this.builder.append(api.baseForm());
        return this;
    }

    /**
     * Add a parameter to the URL
     *
     * @param value
     * @return current url representation
     */
    public URLBuilder withParam(String value) {
        this.builder.append(value);
        return this;
    }

    /**
     * Add a parameter and value to the URL. It will be concatenated with ? or
     * &, depending on whether it is the first parameter or not.
     *
     * @param key
     * @param value
     * @return current url representation
     */
    public URLBuilder withParam(String key, Object value) {
        String concat = isFirstParam() ? "?" : "&";

        String param = String.format("%s%s=%s", concat, key, value);
        this.builder.append(param);
        return this;
    }

    /**
     * Add a parameter and value to the URL. Differently of { withparam},
     * this method will not use ? or & to concatenate the url.
     *
     * @param key
     * @param value
     * @return current url representation
     */
    public URLBuilder withSimpleParam(String key, Object value) {
        this.builder.append(key).append(value);
        return this;
    }

    private boolean isFirstParam() {
        return !builder.toString().contains("?");
    }

    public URLBuilder withParam(Map<String, Object> params) {
        throw new UnsupportedOperationException("Sorry. Not implemented yet.");
    }

    /**
     * Return the url as a string
     *
     * @return final url
     */
    public String build() {
        if(this.builder == null)
            throw new UnsupportedOperationException("No parameter URL has been sent!");
        String concat = isFirstParam() ? "?" : "&";
        if(ForgeModule.oaToken == null){
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please provide your authentication token: ");
                this.oauthToken = new String(BugModule.readPassword());
                ForgeModule.oaToken = this.oauthToken;
        }else{
            this.oauthToken = ForgeModule.oaToken;
        }
        String result = this.builder.append(concat).append(oauthToken).toString();
        this.builder.delete(0, result.length());
        return result;
    }

    public String sbuild() {
        if(this.builder == null)
            throw new UnsupportedOperationException("Nenhum parametro de URL foi enviado!");
        return this.builder.toString();
    }
}