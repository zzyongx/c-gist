Feature: create a simple httpd 

Scenario: create a simple httpd
  Given simple:
  | endpoint                     |
  | http://api.cfs.firstpaas.com/model |
# 
  When DELETE {endpoint}/<prj>/roles/web/modules/httpd
  Then HttpCode 200 JsonMatch '{"code": 0}'
# 
  When DELETE {endpoint}/<prj>/roles/web/modules/base::sysctl
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When DELETE {endpoint}/<prj>/roles/web
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When DELETE {endpoint}/<prj>/roles/base/modules/sysctl
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When DELETE {endpoint}/<prj>/roles/base
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When PUT {endpoint}/<prj>/roles/base With '{"desc": "base role"}'
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When PUT {endpoint}/<prj>/roles/base/modules/sysctl With '{"desc": "sysctl configi"}'
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When GET {endpoint}/<prj>/roles/base/modules/sysctl
  Then HttpCode 200 JsonMatch '{"code": 0}' JsonSave '{"response": {"vsn": "vsn", "fingerprint": "fingerprint"}}'
# 
  When Fun base64_encode With ('# use default 2\\n')
  Then FunSave ('content')
# 
  When PUT {endpoint}/<prj>/roles/base/modules/sysctl/attr With
    """
        {"fingerprint": "__fingerprint__",
         "config": { "file": { "/etc/sysctl1.conf": { "ensure": "file",
                                                      "content": "__content__"
                                                    }
                             }
                   }
        }
    """
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When PUT {endpoint}/<prj>/roles/base/online With '{"vsn": __vsn__}'
  Then HttpCode 200 JsonMatch '{"code": 0, "response": {"vsn": 2}}'
#
  When PUT {endpoint}/<prj>/roles/web
  Then HttpCode 200 JsonMatch '{"code": 0}'
# 
  When PUT {endpoint}/<prj>/roles/web/modules/httpd
  Then HttpCode 200 JsonMatch '{"code": 0}' JsonSave '{"response": {"vsn": "vsn", "fingerprint": "fingerprint"}}'
#
  When PUT {endpoint}/<prj>/roles/web/modules/httpd/attr With
    """
        {"fingerprint": "__fingerprint__",
         "desc": "apache",
         "config": @features/httpd.json
        }
    """
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When PUT {endpoint}/<prj>/roles/web/online With '{"vsn": __vsn__}'
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When PUT {endpoint}/<prj>/roles/web/attr With '{"inherit": "base", "desc": "inherit from base"}'
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When PUT {endpoint}/<prj>/roles/web/inheritmodules/base::sysctl
  Then HttpCode 200 JsonMatch '{"code": 0}' JsonSave '{"response": {"vsn": "vsn", "fingerprint": "fingerprint"}}'
# 
  When Fun base64_encode With ('# use default 2\\n')
  Then FunSave ('content')
#
  When PUT {endpoint}/<prj>/roles/web/inheritmodules/base::sysctl/attr With
    """
        {"fingerprint": "__fingerprint__",
         "config": { "file": { "/etc/sysctl2.conf": { "ensure": "file",
                                                      "content": "__content__"
                                                    }
                             }
                   }
        }
    """
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  When PUT {endpoint}/<prj>/roles/web/online With '{"vsn": __vsn__}'
  Then HttpCode 200 JsonMatch '{"code": 0}'
#
  Examples:
    | prj  |
    | lamp |
