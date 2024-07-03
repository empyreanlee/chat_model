from django.contrib import admin
from django.urls import path
from . import views

views.start_server_thread()
urlpatterns = [
    path("admin/", admin.site.urls),
    path('', views.index, name='index'),
    path('arranca/', views.arranca, name='arranca')
]