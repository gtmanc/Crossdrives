package com.crossdrives.msgraph;

import okhttp3.ResponseBody;
import retrofit2.Callback;

import com.example.crossdrives.R;

public abstract class MeSnippets<Result> extends AbstractSnippet<MSGraphMeService, Result> {
    /**
     * Snippet constructor
     *
     * @param descriptionArray The String array for the specified snippet
     */
    public MeSnippets(Integer descriptionArray) {
        super(SnippetCategory.meSnippetCategory, descriptionArray);
    }

    static MeSnippets[] getMeSnippets() {
        return new MeSnippets[]{
                // Marker element
                new MeSnippets<ResponseBody>(null) {
                    @Override
                    public void request(MSGraphMeService service, Callback callback) {
                        // Not implemented
                    }
                },
                // Snippets

                /* Get information about signed in user
                 * HTTP GET https://graph.microsoft.com/{version}/me
                 * @see https://graph.microsoft.io/docs/api-reference/v1.0/api/user_get
                 */
//                new MeSnippets<ResponseBody>(get_me) {
//                    @Override
//                    public void request(MSGraphMeService service, Callback<ResponseBody> callback) {
//                        service.getMe(getVersion()).enqueue(callback);
//                    }
//                },

                /* Get responsibilities of signed in user
                 * HTTP GET https://graph.microsoft.com/{version}/me?$select=AboutMe,Responsibilities,Tags
                 * @see https://graph.microsoft.io/docs/api-reference/v1.0/resources/user
                 */
//                new MeSnippets<ResponseBody>(get_me_responsibilities) {
//                    @Override
//                    public void request(MSGraphMeService service, Callback<ResponseBody> callback) {
//                        service.getMeResponsibilities(
//                                getVersion(),
//                                SnippetApp.getApp().getString(R.string.meResponsibility)
//                        ).enqueue(callback);
//                    }
//                },

                /* Get the user's manager
                 * HTTP GET https://graph.microsoft.com/{version}/me/manager
                 * @see https://graph.microsoft.io/docs/api-reference/v1.0/resources/user
                 */
//                new MeSnippets<ResponseBody>(get_me_manager) {
//                    @Override
//                    public void request(MSGraphMeService service, Callback<ResponseBody> callback) {
//                        service.getMeEntities(
//                                getVersion(),
//                                SnippetApp.getApp().getString(R.string.manager)
//                        ).enqueue(callback);
//                    }
//                },

                /* Get the user's direct reports
                 * HTTP GET https://graph.microsoft.com/{version}/me/directReports
                 * @see https://graph.microsoft.io/docs/api-reference/v1.0/resources/user
                 */
//                new MeSnippets<ResponseBody>(get_me_direct_reports) {
//                    @Override
//                    public void request(MSGraphMeService service, Callback<ResponseBody> callback) {
//                        service.getMeEntities(getVersion(),
//                                SnippetApp.getApp().getString(R.string.directReports)
//                        ).enqueue(callback);
//                    }
//                },

                /* Get the group membership of the user
                 * HTTP GET https://graph.microsoft.com/{version}/me/memberOf
                 * @see https://graph.microsoft.io/docs/api-reference/v1.0/resources/user
                 */
//                new MeSnippets<ResponseBody>(get_me_group_membership) {
//                    @Override
//                    public void request(MSGraphMeService service, Callback<ResponseBody> callback) {
//                        service.getMeEntities(
//                                getVersion(),
//                                SnippetApp.getApp().getString(R.string.memberOf)
//                        ).enqueue(callback);
//                    }
//                },

                /* Get the photo of the user
                 * HTTP GET https://graph.microsoft.com/{version}/me/userPhoto
                 * @see https://graph.microsoft.io/docs/api-reference/v1.0/resources/user
                 */
                new MeSnippets<ResponseBody>(R.array.get_me_photo) {
                    @Override
                    public void request(MSGraphMeService service, Callback<ResponseBody> callback) {
                        service.getMeEntities(
                                getVersion(),
                                SnippetApp.getApp().getString(R.string.userPhoto)
                        ).enqueue(callback);
                    }
                }
        };
    }
}
