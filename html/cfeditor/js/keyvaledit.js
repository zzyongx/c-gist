function KeyvalEdit()
{
  this.scope = function(o) {
    return null;
  };

  this.input_scope = this.scope;

  this.delimiter = function(scope, loc) {
    return '&nbsp;'
  };

  this.left = function(scope) {
    return "&nbsp;";
  };

  this.middle = this.left;
  this.right = this.left;

  this.pseudo_obj = function(editor, obj) {
    return null;
  };

  this.dumb = function(scope) {
    return null;
  };

  this.inc_indent = function(indent) {
    return indent;
  };

  this.dec_indent = this.dec_indent;
}
