function IniEdit()
{
  this._save = null;

  this.scope = function(o) {
    var scope;
    if (Object.prototype.toString.call(o) == '[object String]') {
      var match = o.match(/^\[(.+)\]$/);
      if (match) {
        scope = '[' + match[1].toLowerCase() + ']';
      } else {
        return null;
      }
    } else {
      scope = '[' + o.scope.toLowerCase() + ']';
    }

    if (this._save == null || this._save != scope) {
      this._save = scope;
      return scope;
    }
    return null;
  };

  this.input_scope = this.scope;

  this.delimiter = function(scope, loc) {
    if (loc == '.left') return this.left(scope);
    if (loc == '.middle') return this.middle(scope);
    if (loc == '.right') return this.right(scope);
    return '&nbsp;'
  };

  this.left = function(scope) {
    return "&nbsp;";
  };

  this.middle = function(scope) {
    return scope ? "&nbsp" : "=";
  };

  this.right = function(scope) {
    return "&nbsp;";
  };

  this.pseudo_obj = function(editor, obj) {
    return {key: "[" + obj.scope + "]", value: '&nbsp;', type: "text",
            id: "", uniq: editor.uniq(), scope: ""};
  };

  this.dumb = function(scope) {
    return null;
  };

  this.inc_indent = function(indent) {
    return indent;
  };

  this.dec_indent = this.inc_indent;
}
