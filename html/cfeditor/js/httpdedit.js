function HttpdEdit()
{
  this._scope = [
    'ifmodule', 'filesmatch', 'directory', 'virtualhost'
  ];

  this.scope = function(o) {
    var key;
    if (Object.prototype.toString.call(o) == '[object String]') {
      key = o.toLowerCase();
    } else {
      key = o.scope.toLowerCase();
    }

    if ($.inArray(key, this._scope) >= 0) {
      return key; 
    } else {
      return null;
    }
  };

  this.input_scope = this.scope;

  this.delimiter = function(scope, loc) {
    if (loc == '.left') return this.left(scope);
    if (loc == '.middle') return this.middle(scope);
    if (loc == '.right') return this.right(scope);
    return '&nbsp;'
  };

  this.left = function(scope) {
    return scope ? "&lt;" : "&nbsp;";
  };

  this.middle = function(scope) {
    return "&nbsp;";
  };

  this.right = function(scope) {
    return scope ? "&gt;" : "&nbsp;";
  };

  this.pseudo_obj = function(editor, obj) {
    return {key: obj.scope, value: obj.key, type: obj.type,
            id: obj.id, uniq: obj.uniq, scope: obj.scope};
  };

  this.dumb = function(scope) {
    if (scope) return '&lt;/' + scope + '&gt;';
    return null;
  };

  this.inc_indent = function(indent) {
    return indent + 1;
  };

  this.dec_indent = function(indent) {
    return indent - 1;
  };
}
