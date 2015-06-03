package org.github.etcd.browser;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.github.etcd.rest.EtcdNode;
import org.github.etcd.rest.EtcdResourceProxy;

public class EditNodePage extends TemplatePage {

    private static final long serialVersionUID = 1L;

    @Inject
    private EtcdResourceProxy proxy;

    private IModel<EtcdNode> editorModel;

    private IModel<Boolean> updating;

    private PageReference returnTo;

    public EditNodePage() {
        super();

        editorModel = Model.of(new EtcdNode());

        updating = Model.of(false);

        initPage();
    }

    public EditNodePage(PageParameters parameters) {
        super(parameters);

        System.out.println("Editing node with key: " + ConvertUtils.getEtcdKey(parameters));

//        initPage();

        editorModel = new LoadableDetachableModel<EtcdNode>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected EtcdNode load() {
                String key = ConvertUtils.getEtcdKey(getPageParameters());
                System.out.println("######### Loading MODEL for EDIT: " + key);
                try {
                    return proxy.getNode(key).getNode();
                } catch (NotFoundException e) {

                    System.err.println("NOT FOUND ... " + e);

                    updating.setObject(false);

                    error("Node with key: " + key + " not found");

                    return new EtcdNode();
                }
            }
        };

        updating = Model.of(true);

        initPage();
    }

    private void initPage() {
        add(new EtcdNodeFormPanel("editor", editorModel, updating) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onFormSave(AjaxRequestTarget target, Form<EtcdNode> form) {
                super.onFormSave(target, form);

                System.out.println("EDITOR MODEL: " + editorModel.getObject());

                if (editorModel.getObject().isDir()) {
                    proxy.createDirectory(editorModel.getObject().getKey(), editorModel.getObject().getTtl(), updating.getObject());
                } else {
                    proxy.createValue(editorModel.getObject().getKey(), editorModel.getObject().getValue(), editorModel.getObject().getTtl(), updating.getObject());
                }

                if (returnTo != null) {
                    setResponsePage(returnTo.getPage());
                } else {
                    setResponsePage(EtcdBrowserPage.class, ConvertUtils.getPageParameters(editorModel.getObject().getKey()));
                }
            }

            @Override
            protected void onFormCancel(AjaxRequestTarget target, Form<EtcdNode> form) {
                super.onFormCancel(target, form);

                if (returnTo != null) {
                    setResponsePage(returnTo.getPage());
                } else {
                    setResponsePage(EtcdBrowserPage.class);
                }
            }
        });
    }
}
