function s() 
{
    return Array.prototype.slice.call(arguments).join('');
}

var editor = {
  error: function() {
    var e = Array.prototype.slice.call(arguments).join('');
    $('#error-display').html($('<p>' + e + '</p>'));
    return false;
  },

  context: {},

  init: function() {
    this._cr_init();
    this._mr_init();
    this._dr_init();

    this._cm_init();
    this._im_init();

    this.context.waitWind = $('#ct-edit-wait');
    this.context.waitWind.hide();
    this.context.outputWind = $('#ct-edit-output');
    this.context.outputWind.hide();

    cfs.get_project();
    this._event_init();
  },

  redraw: function() {
    cfs.get_project();
  },

  _event_init: function() {
    $('#role-list').click(function(event) {
      var fun = event.target.id.split('-', 2);

      switch (fun[0]) {
         case 'cr':
           editor.show_create_role();
           break;
         case 'mr':
           editor.show_modify_role(fun[1]);
           break;
         case 'dr':
           editor.show_delete_role(fun[1]);
           break;
         case 'cm':
           editor.show_create_module(fun[1]);
           break;
         case 'im':
           editor.show_inherit_module(fun[1]);
           break;
      };
    });
  },

  _cr_init: function() {
    var self = this;

    var wind       = $('#ct-cr');
    var name       = wind.find('#cr-name');
    var nameErr    = wind.find('#cr-name-error');
    var inputWind  = wind.find('#ct-cr-input');

    self.context.cr = {wind: wind, input: inputWind, name: name, nameErr: nameErr};

    wind.click(function(event) {
      if (event.target.name == "cr-submit") {
        event.preventDefault();
        editor.create_role();
      } else if (event.target.name == 'cr-cancel') {
        wind.hide();
      }
    });

    name.focus(function(event) {
      self._err_clear(nameErr);
    });

    name.blur(function(event) {
      self._module_name_check(self.context.cm);
    });

    wind.hide();
  },

  _mr_init: function() {
    var self = this;

    var wind       = $('#ct-mr');
    var inputWind  = wind.find('#ct-mr-input');

    self.context.mr = {wind: wind, input: inputWind};

    wind.click(function(event) {
      if (event.target.name == "mr-submit") {
        event.preventDefault();
        editor.modify_role();
      } else if (event.target.name == 'mr-cancel') {
        wind.hide();
      }
    });

    wind.hide();
  },

  _dr_init: function() {
    var self = this;

    var wind       = $('#ct-dr');
    var inputWind  = wind.find('#ct-dr-input');

    self.context.dr = {wind: wind, input: inputWind};

    wind.click(function(event) {
      if (event.target.name == "dr-submit") {
        event.preventDefault();
        editor.delete_role();
      } else if (event.target.name == 'dr-cancel') {
        wind.hide();
      }
    });

    wind.hide();
  },

  _cm_init: function() {
    var self = this;

    var wind       = $('#ct-cm');
    var name       = wind.find('#cm-name');
    var nameErr    = wind.find('#cm-name-error');
    var inputWind  = wind.find('#ct-cm-input');

    self.context.cm = {wind: wind, input: inputWind, name: name, nameErr: nameErr};

    wind.click(function(event) {
      if (event.target.name == "cm-submit") {
        event.preventDefault();
        editor.create_module();
      } else if (event.target.name == 'cm-cancel') {
        wind.hide();
      }
    });

    name.focus(function(event) {
      self._err_clear(nameErr);
    });

    name.blur(function(event) {
      self._role_name_check(self.context.cm);
    });

    wind.hide();
  },

  _im_init: function() {
    var self = this;

    var wind       = $('#ct-im');
    var inputWind  = wind.find('#ct-im-input');

    self.context.im = {wind: wind, input: inputWind};

    wind.click(function(event) {
      if (event.target.name == "im-submit") {
        event.preventDefault();
        editor.inherit_module();
      } else if (event.target.name == 'im-cancel') {
        wind.hide();
      }
    });

    wind.hide();
  },

  _err_clear: function(ct) {
    ct.empty();
  },

  _err_display: function(ct, err) {
    ct.text("* " + err);
  },

  _role_name_check: function(obj) {
    if (obj.name.val() == "") {
      this._err_display(obj.nameErr, "role name can't be empty");
      return false;
    }
    return true;
  },

  _module_name_check: function(obj) {
    if (obj.name.val() == "") {
      this._err_display(obj.nameErr, "module name can't be empty");
      return false;
    }
    return true;
  },

  show_loading: function(obj) {
    obj.wind.hide();
    this.context.waitWind.show();
  },

  show_ok: function(obj) {
    var self = this;

    self.context.outputWind.find('.display').text("OK");
    self.context.outputWind.find('.close').click(function() {
      self.context.outputWind.hide();
    });

    self.context.waitWind.hide();
    self.context.outputWind.show();
    self.context.outputWind.fadeOut(2000);
  },

  show_error: function() {
    if (arguments.length == 1) {
      this._show_error_1(arguments[0]);
    } else if (arguments.length == 2) {
      this._show_error_2(arguments[0], arguments[1]);
    }
  },

  _show_error_1: function(error) {
    var self = this;

    console.log("show error");

    self.context.outputWind.find('.display').text(error);
    self.context.outputWind.find('.close').click(function() {
      self.context.outputWind.hide();
    });

    self.context.outputWind.show();
  },

  _show_error_2: function(obj, error) {
    var self = this;

    self.context.outputWind.find('.display').text(error);
    self.context.outputWind.find('.close').click(function() {
      self.context.outputWind.hide();
      obj.wind.show();
    });

    self.context.waitWind.hide();
    self.context.outputWind.show();
  },

  //{"code":0,"response":[{"role":"web","vsn":3,"evsn":3,"desc":"inherit from base","timestamp":"2014-03-10 10:12:24","modules":["base::sysctl","httpd"],"parent":"base","children":[]},{"role":"base","vsn":2,"evsn":2,"desc":"base role","timestamp":"2014-03-10 10:12:23","modules":["sysctl"],"parent":null,"children":["web"]}]}

  draw: function(roles) {
    var rlist = $('#role-list');
    rlist.html(s('<div class="command bline">',
                 '<input type="button" id="cr" value="create role" />',
                 '</div>'));
    $.each(roles, function(i, role) {
      var trunk = $(s('<div id="role-list-', role.role, '" class="ct-role bline"></div>'))
        .append($(s('<h2 id="', role.role, '">', role.role, '</h2>')));

      if (role.parent != null) {
        trunk.append($(s('<h3> parent: <span>', role.parent, '</span></h3>')));
      }

      trunk.append($(s('<h3> vsn: <span>', role.vsn, '</span></h3>')))
        .append($(s('<h3> description: <span>', role.desc, '</span></h3>')));

      if (role.modules.length > 0) {
        trunk.append($(s('<h3> modules: </h3>')));
        var p = $(s('<ul class="module-list"></ul>'));
        $.each(role.modules, function(i, module) {
          p.append($(s('<li id="', role.role, "-", module, '">',
                        module, '</li>')));
        });
        trunk.append(p);
      }

      if (role.children.length > 0) {
        trunk.append($(s('<h3> childrens: </h3>')));
        var p = $(s('<ul class="module-list"></ul>'));
        $.each(role.children, function(i, child) {
          p.append($(s('<li> <a href="#', child, '">', child, '</a></li>')));
        });
        trunk.append(p);
      }

      trunk.append(function() {
        return $(s('<div class="command"></div>'))
          .append($(s('<input type="button" value="modify role" id="mr-',
                      role.role, '" />')))
          .append($(s('<input type="button" value="delete role" id="dr-',
                      role.role, '" />')))
          .append($(s('<input type="button" value="create module" id="cm-',
                      role.role, '" />')))
          .append($(s('<input type="button" value="inherit module" id="im-',
                      role.role, '" />')));
          }())
        .appendTo(rlist);
    });
  },

  _init_inherit_role: function(obj, roles) {
    obj.empty();
    $.each(roles, function(i, role) {
      var checked = role.asparent ? 'checked="checked"' : '';
      var label = role.name == "0" ? "no parent" : role.name;
      obj.append($(s('<input type="radio" name="r-inherit" value="',
                     role.name, '"', checked, '" />')))
         .append($(s('<label>', label, '</label>')));
    });
  },

  _init_create_role_wind: function(obj) {
    obj.name.val("");
    obj.wind.find('#cr-desc').val("");
    this._init_inherit_role(obj.wind.find('#ct-cr-inherit'), cfs.get_inherit_roles());
  },

  show_create_role: function() {
    var obj = this.context.cr;
    this._init_create_role_wind(obj);
    obj.wind.show();
  },

  create_role: function() {
    var obj = this.context.cr;
    if (!this._role_name_check(obj)) return false;

    var parent = obj.wind.find("input:radio[name=r-inherit]:checked").val();
    parent = parent == "0" ? null : parent;

    cfs.create_role(obj, obj.name.val(), {
                      desc: obj.wind.find('#cr-desc').val(),
                      inherit: parent});
  },

  _init_modify_role_wind: function(obj, roleName) {
    var roleObj = cfs.get_role(roleName);
    obj.wind.find('#mr-name').text(roleName);
    obj.wind.find('#mr-desc').val(roleObj.desc);
    this._init_inherit_role(obj.wind.find('#ct-mr-inherit'),
                            cfs.get_inherit_roles(roleName));
  },

  show_modify_role: function(role) {
    var obj = this.context.mr;
    this._init_modify_role_wind(obj, role);
    obj.wind.show();
  },

  modify_role: function() {
    var obj = this.context.mr;

    var parent = obj.wind.find("input:radio[name=r-inherit]:checked").val();
    parent = parent == "0" ? null : parent;

    cfs.modify_role(obj, obj.wind.find('#mr-name').text(), {
                      desc: obj.wind.find('#mr-desc').val(),
                      inherit: parent});
  },

  _init_delete_role_wind: function(obj, roleName) {
    var roleObj = cfs.get_role(roleName);
    obj.wind.find('#dr-name').text(roleName);
    obj.wind.find('#dr-desc').val(roleObj.desc);
  },

  show_delete_role: function(role) {
    var obj = this.context.dr;
    this._init_delete_role_wind(obj, role);
    obj.wind.show();
  },

  delete_role: function() {
    var obj = this.context.dr;
    cfs.delete_role(obj, obj.wind.find('#dr-name').text());
  },

  _init_create_module_wind: function(obj, roleName) {
    obj.wind.find('#cm-role-name').text(roleName);
    obj.name.val("");
    obj.wind.find('#cm-desc').val("");
  },

  show_create_module: function(role) {
    var obj = this.context.cm;
    this._init_create_module_wind(obj, role);
    obj.wind.show();
  },

  create_module: function() {
    var obj = this.context.cm;
    if (!this._module_name_check(obj)) return false;

    cfs.create_module(obj, obj.wind.find('#cm-role-name').text(), obj.name.val(),
                      {desc: obj.wind.find('#cm-desc').val()});
  },

  _init_inherit_module: function(obj, modules) {
    obj.empty();
    $.each(modules, function(i, module) {
      obj.append($(s('<input type="radio" name="m-inherit" value="',
                     module, '" />')))
         .append($(s('<label>', module, '</label>')));
    });
  },

  _init_inherit_module_wind: function(obj, roleName) {
    obj.wind.find('#im-role-name').text(roleName);

    var imodules = cfs.get_inherit_modules(roleName);
    if (imodules.length == 0) return false;

    this._init_inherit_module(obj.wind.find('#ct-im-inherit'), imodules);
    obj.wind.find('#im-desc').val("");
    return true;
  },

  show_inherit_module: function(role) {
    var obj = this.context.im;
    if (this._init_inherit_module_wind(obj, role)) {
      obj.wind.show();
    } else {
      this.show_error("no parent role");
    }
  },

  inherit_module: function() {
    var obj = this.context.im;

    var module = obj.wind.find("input:radio[name=m-inherit]:checked").val();
    cfs.inherit_module(obj, obj.wind.find('#im-role-name').text(), module,
                       {desc: obj.wind.find('#im-desc').val()});
  }

};

var cfs = {
  context: {},

  options: {endpoint: "http://cfs.api.firstpaas.com/model"},

  init: function(opts) {
    $.extend(this.options, opts);
  },
  
  get_project: function() {
    uri = s(this.options.endpoint, "/", this.options.prj, "/roles");
    self = this;
    $.get(uri, function(data) {
      json = JSON.parse(data);
      if (json.code == 10101) {
        editor.draw([]);
      } else if (json.code == 0) {
        self.context.prj = json.response;
        editor.draw(json.response);
      } else {
        editor.error(json.code, json.response);
      }
    }).fail(function() {
      editor.error(this.statusText, this.responseText);
    })
  },

  get_inherit_modules: function(roleName) {
    var prj = this.context.prj;

    var parent = null;
    var nowinherit = new Array();
    for (var i = 0; i < prj.length; i++) {
      var obj = prj[i];
      if (obj.role == roleName) {
        parent = obj.parent;
        $.each(obj.modules, function(i, m) {
          if (m.match(/::/)) nowinherit.push(m);
        });
        break;
      }
    }

    var modules = new Array();
    var getParentModules = function(role) {
      if (role == null) return true;
      for (var i = 0; i < prj.length; i++) {
        var obj = prj[i];
        if (obj.role == role) {
          $.each(obj.modules, function(i, m) {
            var qm = role + "::" + m;
            if ($.inArray(qm, nowinherit) < 0) modules.push(qm);
          });
          break;
        }
      }
      getParentModules(role.parent);
    };

    if (parent) getParentModules(parent);
    return modules;
  },

  get_inherit_roles: function() {
    var parentnow = null;
    var roleName = arguments.length == 1 ? arguments[0] : null;

    var roleObj = roleName ? this.get_role(roleName) : {parent: null, children: []};

    var roles = new Array();
    roles.push({name: "0", asparent: roleObj.parent == null});

    $.each(this.context.prj, function(i, role) {
      if (role.role != roleName && $.inArray(role.role, roleObj.children) < 0) {
        roles.push({name: role.role, asparent:role.role == roleObj.parent});
      }
    });

    return roles;
  },

  get_role: function(role) {
    for (var i = 0; i < this.context.prj.length; i++) {
      if (this.context.prj[i].role == role) {
        return this.context.prj[i];
      }
    }
  },

  _http_request: function(obj, request) {
    var def = {
      success: function(data) {
        json = JSON.parse(data);
        if (json.code == 0) {
          editor.show_ok(obj);
          editor.redraw();
        } else {
          editor.show_error(obj, data);
        }
        return true;
      },
      error: function(jqXHR, textStatus, errorThrown) {
        editor.show_error(obj, textStatus + " " +  errorThrown);
      }
    };

    var req = $.extend(def, request)

    $(document).ajaxStart(function() { editor.show_loading(obj) });
    $.ajax(req);
    $(document).ajaxStart(function(){});
  },

  create_role: function(obj, role, attr) {
    uri = s(this.options.endpoint, "/", this.options.prj, "/roles/", role);
    this._http_request(obj, {url: uri, data: JSON.stringify(attr), type: 'PUT'});
  },

  modify_role: function(obj, role, attr) {
    uri = s(this.options.endpoint, "/", this.options.prj, "/roles/", role, "/attr");
    this._http_request(obj, {url: uri, data: JSON.stringify(attr), type: 'PUT'});
  },

  delete_role: function(obj, role) {
    uri = s(this.options.endpoint, "/", this.options.prj, "/roles/", role);
    this._http_request(obj, {url: uri, type: 'DELETE'});
  },

  create_module: function(obj, role, module, attr) {
    uri = s(this.options.endpoint, "/", this.options.prj,
            "/roles/", role, "/modules/", module);
    this._http_request(obj, {url: uri, data: JSON.stringify(attr), type: 'PUT'});
  },

  inherit_module: function(obj, role, module, attr) {
    uri = s(this.options.endpoint, "/", this.options.prj,
            "/roles/", role, "/inheritmodules/", module);
    this._http_request(obj, {url: uri, data: JSON.stringify(attr), type: 'PUT'});
  },

  /*
  get_module: function(obj, role, module) {
  
  },
  */
};


$(document).ready(function() {
  var matchs = location.pathname.match("/editor/([^\/]+)$");
  if (!matchs) {
    return editor.error("invalid uri ", location.href);
  }

  var project = matchs[1];
  cfs.init({'prj' : project});

  editor.init();
  $('#role-list').hide();

  configedit_test();
});
