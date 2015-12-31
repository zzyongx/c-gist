test-bdd-perl
=============
base on bdd but more simple and codeless

usage
=====
1. write feature file, put in feature directory
2. run ./bdd

feature file grammar
====================
When _httpmethod_ uri
When _httpmethod_ uri With '_httpcontent_'
When _httpmethod_ uri With:_encode_ '_httpcontent_'
When _fun_ _funname_ With ('fun_param1' 'fun_param2')

Then HttpCode httpcode JsonMatch '_jsonmatch_'
Then HttpCode httpcode JsonMatch '_jsonmatch_' JsonSave '_jsonsave_'
Then FunSave ('funreturn1', 'funreturn2')

_httpmethod_ in [GET, PUT, POST, DELETE]
_encode_ in [form] is content_type

_fun_ in [Fun]
_funname_ in [base64_encode]

_jsonmatch_ is a Json skeleton,
return value may have lots of object(fields)/array(elements),
but only the appoint fields/elements matchs, return is ok. example:
{"response": {"id": 9}}, if the response's id is 9, ok
["*", "*", 9], if the third element is 9, ok

_jsonsave_ is a Json skeleton,
save return value'corresponding fields/elements. for example:
{"response": {"id": "id"}}, the response's id is saved, use "id" access it
["*", "*", "color"] the third element is saved, use "color" access it

_httpcontent_
if have __name__, will be replaced by the value saved by name
if have @file, will be replaced by the content of file

{name}, name is global value in Given
<name>, name is example value in Examples

example
=======
@see features/example.feature

deps
====
Test::BDD::Cucumber
Path::Class
Moose
Devel::Declare
Mouse
Devel::Pragma
