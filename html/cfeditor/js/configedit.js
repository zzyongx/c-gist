function ConfigEdit(opts)
{
  var ifor = function(If, or) {
    return If ? If : or
  };

  this._format = ifor(opts.format, "raw");
  this._container = ifor(opts.container, "configedit");
  this._config = ifor(opts.config, []);

  this._uniq = 0;
  this._history = new LimitArray(100);

  this._lnWind = null;
  this._wind = null;
  this._cmdWind = null;

  this._editWind = null;
  this._editUniq = 0;

  this._viewWind = null;

  this._optionWind = null;
  this._addMode = false;

  this._moveWind = null;
  this._moveFrom = null;

  this._x        = null;
  this._recvWind = null;

  this._simpleView = false;

  /* api */

  this.dump = function() {
    this._encode();
    return this._config;
  };

  this.uniq = function() {
    this._uniq;
  }

  this._init = function() {
    this._decode();
    this._init_wind();

    if (this._format == "raw") {
      this._raw_init();
    } else {
      if (this._format == "httpd") {
        this._format_impl = new HttpdEdit();
      } else if (this._format == "ini") {
        this._format_impl = new IniEdit();
      } else if (this._format == "keyval") {
        this._format_impl = new KeyvalEdit();
      }

      this._format_init();
    }
    this._event_init();
  };

  this._raw_init = function() {
    $('<textarea id="configedit-raw"></textarea>').appendTo(this._wind);
    this._wind.find('#configedit-raw').val(this._config);
  };

  this._is_array = function(o) {
    return Object.prototype.toString.call(o) === '[object Array]';
  };

  this._is_string = function(o) {
    return Object.prototype.toString.call(o) === '[object String]';
  };

  this._sprintf = function() {
    var format = function(p) {
      var f = p[0];
      for (var i = 1; i < p.length; ++i) {
        var regex = new RegExp(p[i][0], "g");
        f = f.replace(regex, p[i][1]);
      }
      return f;
    };

    var strings = new Array();
    for (var i = 0; i < arguments.length; ++i) {
      strings.push(format(arguments[i]));
    }
    return strings.join('');
  };

  this._refresh = function() {
    var num = 0;
    var indent = 0;
    var self = this;

    this._wind.children('.view').each(function() {
      var node = $(this);
      node.removeClass('alt');
      if (num++ % 2 == 0) {
        node.addClass('alt');
      }

      node.data("ln", num);

      if (self._simpleView) {
        node.find('.option').hide();
        node.find('.id').hide();
        node.find('.type').hide();
      } else {
        node.find('.option').show();
        node.find('.id').show();
        node.find('.type').show();
        if (node.find('.type').text() == "text") {
          node.find('.type').html('&nbsp;');
        }
      }

      var mleft = indent;

      if (node.data('scope') != "") {
        indent = self._format_impl.inc_indent(indent);
      } else if (node.attr('id').search(/^configedit-viewend/) >= 0) {
        indent = self._format_impl.dec_indent(indent);
        mleft = indent;
      }

      node.find('.indent').css({
        'margin-left': (mleft * 10) + 'px'
      });
    });

    if (self._simpleView) {
      this._wind.css({'margin-left': '2px'});
      this._lnWind.hide();
    } else {
      var width = (num.toString().length) * 6 + 6;
      this._lnWind.css({width: width + 'px'});
      this._wind.css({'margin-left': width + 'px'});

      this._lnWind.empty();

      for (var i = 0; i < num; ++i) {
        this._lnWind.append($('<div >' + (i+1) + '</div>'));
      }
      this._lnWind.show();
    }
  };

  this._show_edit = function(uniq) {
    if (this._editWind) this._save_line();

    this._viewWind = this._wind.find('#configedit-view-' + uniq);
    if (this._editWind) {
      this._editWind = this._refresh_edit(this._editWind, this._viewWind, uniq);
    } else {
      this._editWind = this._create_edit(this._viewWind, uniq);
      this._eventize_edit(this._editWind);
    }

    this._editUniq = uniq;

    var self = this;
    $.each(['.key', '.value', '.type', '.id'], function(i, e) {
      self._editWind.find(e).val(self._viewWind.find(e).text());
    });

    this._editWind.show();
    var self = this;
    setTimeout(function() { self._editWind.find(".key").focus(); }, 0);
  };

  this._hide_edit = function(save) {
    if (this._editWind) {
      if (save) {
        this._save_line();
      } else {
        if (this._addMode) {
          this._viewWind.remove();
          this._refresh();
        }
      }

      this._addMode = false;
      this._editWind.remove();
      this._editWind = this._viewWind = null;
    }
  };

  this._toggle_editid = function() {
    if (this._editWind.find('.id').data("order") == "last") {
      this._editWind.find('.id').data("order", "").hide();
      this._editWind.find('.type').data("order", "last");
    } else {
      this._editWind.find('.id').data("order", "last").show();
      this._editWind.find('.type').data("order", "");
    }
  };

  this._parse_event = function(id) {
    var ev = {action: null};
    var e = id.split("-");
    if (e.length >= 3) {
      if (e.slice(0, 2).join('-') == "configedit-view") {
        ev = {action: "view", uniq: e[2]};
      } else if (e.slice(0, 2).join('-') == "configedit-viewend") {
        ev = {action: "viewend", uniq: e[2]};
      }
      if (e.length == 4) ev.zone = e[3];
    }

    if (ev.action == null) console.log(id, "action null, something may wrong");
    return ev;
  };

  this._save_line = function() {
    var oldScope = this._viewWind.data("scope");
    if (oldScope == "" || oldScope == undefined) oldScope = null;

    var scope = this._format_impl.input_scope(this._editWind.find(".key").val());
    if (scope) {
      this._editWind.find(".key").val(scope);
      this._viewWind.data("scope", scope);
    } else {
      this._viewWind.data("scope", "");
    }

    var self = this;
    $.each([".key", ".value", ".type", ".id"], function(i, k) {
      self._viewWind.find(k).text(self._editWind.find(k).val());
    });

    $.each([".left", ".middle", ".right"], function(i, k) {
      self._viewWind.find(k).html(self._format_impl.delimiter(scope, k));
    });

    var id = '#configedit-viewend-' + this._editUniq;
    // console.log("save line ", id, scope, oldScope);

    if (this._addMode || !oldScope) {  /* scope not exists, add */
      if (scope && self._wind.find(id).length == 0) {
        console.log("added");
        var dumb = this._format_impl.dumb(scope);
        if (dumb) {
          $(this._create_dumb_line(this._editUniq, dumb))
            .insertAfter(this._editWind);
        }
      }
    } else if (scope && scope != oldScope) {  /* scope exists, replace */
      console.log("replace");
      var dumb = this._format_impl.dumb(scope)
      if (dumb) {
        $(this._create_dumb_line(this._editUniq, dumb))
          .replaceAll(this._wind.find(id));
      }
    } else if (oldScope && oldScope != scope) {  /* scope null, delete */
      console.log("delete");
      $(id).remove();
    }

    this._refresh();
  };

  this._next_line = function() {
    this._save_line();
    if (this._addMode) {
      this._create_next_line();
    } else {
      this._edit_next_line();
    }
  };

  this._create_next_line = function() {
      this._add_line(this._editUniq);
  };

  this._edit_next_line = function() {
    var searchWind = this._viewWind;
    while (true) {
      var next = searchWind.next();
      if (next.length == 0) {
        searchWind = $(this._wind.children()[0]);
      } else {
        searchWind = next;
      }

      var id = searchWind.attr("id");
      if (id) {
        var ev = this._parse_event(id);
        if (ev.action == "view") {
          this._show_edit(ev.uniq);
          break;
        }
      }
    }
  };

  this._edit_prev_line = function() {
    this._save_line();

    var searchWind = this._viewWind;
    while (true) {
      var prev = searchWind.prev();
      if (prev.length == 0) {
        var children = this._wind.children();
        searchWind = $(children[children.length-1]);
      } else {
        searchWind = prev;
      }

      var id = searchWind.attr("id");
      if (id) {
        var ev = this._parse_event(id);
        if (ev.action == "view") {
          this._show_edit(ev.uniq);
          break;
        }
      }
    }
  };

  this._create_edit = function(wind, uniq) {
    var htmls = [
      '<div id="configedit-edit" data-uniq="_ID_" class="edit">',
        '<label for="configedit-edit-key">',
          '<span class="label">key:</span>',
        '</label>',
        '<input type="text" id="configedit-edit-key" class="key" data-order="first" />',
        '<br />',

        '<label for="configedit-edit-value">',
          '<span class="label">value:</span>',
        '</label>',
        '<textarea id="configedit-edit-value" class="value"></textarea>',
        '<br />',

        '<label for="configedit-edit-type">',
          '<span class="label">type:</span>',
        '</label>',
        '<select id="configedit-edit-type" class="type" data-order="last">',
          '<option value ="text">text</option>',
          '<option value ="var">var</option>',
          '<option value ="raw">raw</option>',
        '</select>',
        '<br />',

        '<label for="configedit-edit-id">',
          '<span class="label editid" id="configedit-edit-editid">id: </span>',
        '</label>',
        '<input type="text" id="configedit-edit-id" class="id hidden" />',
      '</div>'
    ];

    return $(this._sprintf([htmls.join(''), ['_ID_', uniq]])).insertAfter(wind);
  };

  this._refresh_edit = function(editWind, viewWind, uniq) {
    editWind.data('uniq', uniq);
    editWind.find('.id').data("order", "").hide();
    editWind.detach().insertAfter(viewWind).hide();
    return editWind;
  };

  this._eventize_edit = function(editWind) {
    var self = this;

    editWind.click(function(event) {
      if (event.target.id == "configedit-edit-editid") {
        self._toggle_editid();
      }
      event.stopPropagation();
    });

    /* keypress is not working in chrome */
    editWind.keydown(function(event) {
      var order = $(event.target).data("order");
      if (event.keyCode == 9 && order == "last") {
        self._next_line();
      } else if (event.keyCode == 9 && event.shiftKey && order == "first") {
        self._edit_prev_line();
      } else if (event.keyCode == 27) {
        self._hide_edit(false);
      } else if (event.keyCode == 13 && order == "last") {
        self._hide_edit(true);
      }
      event.stopPropagation();
    });

    var input_onfocus = function() {
      $(this).select();
    };

    editWind.find("input").focus(input_onfocus);
    editWind.find("textarea").focus(input_onfocus);
  };

  this._create_view_line = function(obj, scope) {
    var left   = this._format_impl.left(scope);
    var middle = this._format_impl.middle(scope);
    var right  = this._format_impl.right(scope);

    var html = this._sprintf(
      ['<div id="configedit-view-_ID_" data-scope="_SCOPE_" class="view">',
       ['_ID_', obj.uniq], ['_SCOPE_', obj.scope]],
      ['<span class="indent">&nbsp;</span>'],
      ['<span class="left"> _LEFT_ </span>',
       ['_LEFT_', left]],
      ['<span class="key editable" data-uniq="_ID_">_KEY_</span>',
       ['_ID_', obj.uniq], ['_KEY_', obj.key]],
      ['<span class="middle"> _MIDDLE_ </span>',
       ['_MIDDLE_', middle]],
      ['<span class="value editable" data-uniq="_ID_">_VAL_</span>',
       ['_ID_', obj.uniq], ['_VAL_', obj.value]],
      ['<span class="right"> _RIGHT_ </span>',
       ['_RIGHT_', right]],
      ['<span id="configedit-view-_ID_-option" class="option"> &or; </span>',
       ['_ID_', obj.uniq]],
      ['<span class="id">_UID_</span>',
       ['_UID_', obj.id == "" ? "&nbsp" : obj.id]],
      ['<span class="type">_TYPE_</span>',
       ['_TYPE_', obj.type]],
      ['</div>']);

    return html;
  };

  this._create_dumb_line = function(uniq, val) {
    var htmls = [
      '<div id="configedit-viewend-_ID_" data-scope="" class="view">',
        '<span class="indent">&nbsp;</span>',
        '<span> _VAL_ </span>',
      '</div>'
    ];
    return this._sprintf([htmls.join(''), ['_ID_', uniq], ['_VAL_', val]]);
  };

  this._wait_recv = function(x) {
    var ox = x;
    x -= 0.5;
    x = x.toFixed();
    if (this._x !== null) {
      if (x == this._x) return;
    }
    this._x = x;

    /* remove first, it's children too */
    if (this._recvWind) this._recvWind.remove();

    var winds = this._wind.children('.view');
    var mx;
    for (var i = 0; i < winds.length; ++i) {
      if ($(winds[i]).attr("id") == this._moveWind.attr("id")) {
        mx = i;
        break;
      }
    }

    if (x < 0) x = -1;
    if (x >= winds.length-1) x = winds.length -2;
    if (x == 0) x = 0;  /* x == -0 */

    var html = '<div id="configedit-waitrecv" class="view recv"></div>';

    if (x == -1) {
      this._recvWind = $(html).insertBefore(winds[0]);
    } else if (x <= mx) {
      this._recvWind = $(html).insertAfter(winds[x]);
    } else {
      this._recvWind = $(html).insertAfter(winds[x]);
    }

    // console.log("loc:", x, "oloc", ox, this._recvWind, winds.length);
  };

  this._add_line = function(uniq) {
    this._addMode = true;

    console.log("add line after ", uniq);

    var nextUniq = uniq >= 0 ? this._uniq+1: this._uniq;
    var obj = {uniq: nextUniq, key: "Key", value: "Value",
               type: "Text", id: "", scope: ""};

    if (uniq >= 0) {
      $(this._create_view_line(obj))
        .insertAfter(this._wind.find('#configedit-view-' + uniq));
    } else {
      $(this._create_view_line(obj))
        .insertBefore(this._wind.find('#configedit-end'));
    }

    this._show_edit(nextUniq);
    this._refresh();
    this._uniq++;
  };

  this._remove_line = function(uniq) {
    this._wind.find('#configedit-view-' + uniq).remove();
    this._wind.find('#configedit-viewend-' + uniq).remove();

    this._refresh();
  };

  this._show_option = function(event, uniq) {
    this._hide_option();

      var menu = [
        '<div id="configedit-option" class="options"><ul>',
          '<li id="configedit-option-add" data-uniq="_ID_">add</li>',
          '<li id="configedit-option-remove" data-uniq="_ID_">remove</li>',
          '<li id="configedit-option-edit" data-uniq="_ID_">edit</li>',
        '</ul></div>'
      ];

    var html = this._sprintf([menu.join(''), ['_ID_', uniq]]);

    this._optionWind = $(html)
        .insertAfter(this._wind.find('#configedit-end'))
        .css({left: (event.pageX - 78)+ "px",
              top:  (event.pageY - 12) + "px"});

    var self = this;
    this._optionWind.click(function(event) {
      self._hide_option();
      id = $(this).data("uniq");
      switch (event.target.id) {
        case "configedit-option-add":
          self._add_line(uniq);
          break;
        case "configedit-option-remove":
          self._remove_line(uniq);
          break;
        case "configedit-option-edit":
          self._show_edit(uniq);
          break;
      }
      event.stopPropagation();
    });
  };

  this._hide_option = function() {
    if (this._optionWind) {
      this._optionWind.remove();
    }
  }

  this._format_init = function() {
    var main = '';
    var self = this;

    var cons_line = function(config) {
      $.each(config, function(i, obj) {
        if (self._is_array(obj.value) ||
            (self._is_string(obj.scope) && obj.scope != "")) {
          var scope = self._format_impl.scope(obj);
          if (scope) {
            var newObj = self._format_impl.pseudo_obj(self, obj);
            main += self._create_view_line(newObj, scope);

            if (self._is_array(obj.value)) {
              cons_line(obj.value);
            } else {
              main += self._create_view_line(obj);
            }

            var dumb = self._format_impl.dumb(scope);
            if (dumb) {
              main += self._create_dumb_line(obj.uniq, dumb);
            }
          } else {
            if (self._is_array(obj.value)) {
              cons_line(obj.value);
            } else {
              main += self._create_view(obj);
            }
          }
        } else {
          main += self._create_view_line(obj);
        }
      });
    };

    cons_line(this._config);
    main += '<div id="configedit-end" class="clearfix"></div>';
    this._wind.html(main);
    
    this._refresh();
  };

  this._init_wind = function() {
    var main = $('#' + this._container);
    this._lnWind = $('<div id="configedit-ln"></div>').appendTo(main);
    this._wind = $('<div id="configedit-main"></div>').appendTo(main);

    var menu = [
      '<div id="configedit-cmd" class="clearfix">',
        '<ul>',
          '<li id="configedit-cmd-add">add</li>',
          // '<li id="configedit-cmd-import">import</li>',
          '<li id="configedit-cmd-view">view</li>',
        '</ul>',
        '<div class="clearfix"></div>',
      '</div>',
    ];

    this._cmdWind = $(menu.join('')).appendTo(main);
    $('<div class="clearfix"></div>').appendTo($('#' + this._container));
  };

  this._event_init = function() {
    var self = this;

    this._wind.click(function(event) {
      if (event.target.id) {
        var ev = self._parse_event(event.target.id);
        if (ev.action == "view") {
          self._hide_option();
          self._hide_edit();

          if (ev.zone == "option") {
            self._show_option(event, ev.uniq);
          }
        }
      }
      event.stopPropagation();
    });

    this._wind.dblclick(function(event) {
      var node = $(event.target);
      if (node.hasClass("editable") && node.data("uniq") != undefined) {
        self._show_edit(node.data("uniq"));
        event.stopPropagation();
      }
    });

    this._wind.mousedown(function(event) {
      var id = event.target.id;
      if (id) {
        var ev = self._parse_event(id);
        if ((ev.action == "view" || ev.action == "viewend") && !ev.zone) {
          self._moveWind = self._wind.find('#' + event.target.id);
          self._moveWind.addClass('mousedown');
          if (self._moveWind.prev().length) {
            self._moveFrom = { prev: self._moveWind.prev() };
          } else if (self._moveWind.next().length) {
            self._moveFrom = { next: self._moveWind.next() };
          } else {
            self._moveFrom = { parent: self._moveWind.parent() };
          }
          self._moveFrom.id = id;
        }
      }
      event.stopPropagation();
    });

    this._wind.mouseup(function(event) {
      if (self._moveWind) {
        self._moveWind.removeClass('mousedown');
        self._moveWind.removeAttr('style');

        if (self._recvWind) {
          self._recvWind.replaceWith(self._moveWind);
          self._refresh();
        } else {
          if (self._moveFrom.prev) {
            self._moveWind.insertAfter(self._moveFrom.prev);
          } else if (self._moveFrom.next) {
            self._moveWind.insertBefore(self._moveFrom.next);
          } else {
            self._moveWind.prependTo(self._moveFrom.parent);
          }
        }

        self._x = null;
        self._recvWind = null;
        self._moveWind = self._moveFrom = null;

      }
      event.stopPropagation();
    });

    this._wind.mousemove(function(event) {
      if (self._moveWind) {
        var width = parseInt(self._moveWind.parent().css('width'));
        var offsetX = width/2;
        var offsetY = parseInt(self._moveWind.css('height'))/2;

        self._moveWind.css({
          // 'z-index': -1,
          position: 'absolute',
          width: width + 'px',
          'background-color': 'lightgrey', 
          left: (event.pageX - offsetX) + 'px',
          top:  (event.pageY - offsetY) + 'px'
        });

        var x = (event.pageY - self._wind.offset().top) / 
                  parseInt(self._moveWind.height());
        self._wait_recv(x);
      }
      event.stopPropagation();
    });

    /* mouseover useless */
    this._wind.mouseover(function(event) {
      event.stopPropagation();
    });

    /* mouseout useless */
    this._wind.mouseout(function(event) {
      event.stopPropagation();
    });

    this._cmdWind.click(function(event) {
      var id = event.target.id;
      if (id == "configedit-cmd-add") {
        self._add_line(-1);
      } else if (id == "configedit-cmd-view") {
        self._simpleView = !self._simpleView;
        self._refresh();
      }
      event.stopPropagation();
    });

    $(document).click(function() {
      self._hide_option();
      self._hide_edit();
    });
  };



  this._decode = function() {
    if (this._format == "raw") {
      this._config = $.base64.decode(this._config);
    } else {
      var self = this;
      var fun = function(config) {
        var r = new Array();
        $.each(config, function(i, obj) {
          if (self._is_array(obj.value)) {
            obj.value = fun(obj.value);
          } else if (obj.type == "raw") {
            obj.value = $.base64.decode(obj.value);
          }
          if (obj.type == undefined) obj.type = "";
          if (obj.scope == undefined) obj.scope = "";
          if (obj.id == undefined) obj.id = "";
          obj.uniq = self._uniq++;
          r.push(obj);
        });
        return r;
      };
      self._config = fun(self._config);
    }
  };

  this._encode = function() {
    if (this._format == "raw") {
      this._config = $.base64.encode(this._config);
    } else {
      var self = this;
      var fun = function(config) {
        var r = new Array();
        $.each(config, function(i, obj) {
          if (self._is_array(obj.value)) {
            obj.value = fun(obj.value);
          } else if (obj.type == "raw") {
            obj.value = $.base64.encode(obj.value);
          }
          r.push(obj);
        });
        return r;
      };
      self._config = fun(self._config);
    }
  };

  /* GoOn construct */
  this._init();
}

function LimitArray(limit)
{
  this._limit = limit;
  this._array = new Array();

  this.push = function(value) {
    this._array.push(value);
    if (this._array.length > this._limit) {
      this._array.shift();
    }
  };

  this.at = function(index) {
    return this._array[index];
  };

  this.array = function() {
    return this._array;
  };
}

function configedit_test()
{
  /*
  var cfeditor = new ConfigEdit({format:"raw",
                                 container: "configedit",
                                 config: $.base64.encode("raw content")});
  */

  var config = [
    {key: "KeepAlive", value: "Off"},
    {key: "KeepAliveTimeout", value: 15, id:199},
    {key: null, value: $.base64.encode("raw data"), type: "raw"},
    {key: "prefork.c", value: [
        {key: "StartServers", value: 8},
        {key: "MinSpareServers", value: 20},
        {key: "MaxSpareServers", value: 50},
        {key: "ServerLimit", value: "httpd::server_limit", type: "var"},
        {key: "MaxClients", value: "httpd::max_clients", type: "var"},
      ], scope: "ifmodule"}
  ];

  var cfeditor = new ConfigEdit({format: "httpd", config: config});

  /*
  var config = [
    {key: "max_execution_time", value: 30},
    {key: "max_input_time", value: "php::max_input_time", type: "var"},
    {key: "memory_limit", value: "384M"},
    {key: "mysql.connect_timeout", value: 60, scope: "mysql"},
    {key: "mysql.trace_mode", value: "Off", scope: "mysql"}
  ];

  var cfeditor = new ConfigEdit({format: "ini", config: config})

  var config = [
    {key: "list-max-ziplist-entries", value: 512},
    {key: "list-max-ziplist-value", value: 64},
    {key: "zset-max-ziplist-entries", value: 128},
    {key: "zset-max-ziplist-value", value: 64}
  ];

  var cfeditor = new ConfigEdit({format: "keyval", config: config})
  */
}
